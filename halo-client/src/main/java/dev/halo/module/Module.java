package dev.halo.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), VISUAL("Visual"), HUD("HUD"), MOVEMENT("Movement");
        public final String label;
        Category(String label) { this.label = label; }
    }

    // ---------- settings ----------
    public static abstract class Setting {
        public final String name;
        public Runnable onChange = () -> {};
        protected Setting(String name) { this.name = name; }
        public abstract JsonElement save();
        public abstract void load(JsonElement e);
    }

    public static final class Bool extends Setting {
        public boolean value;
        public float anim;
        public Bool(String n, boolean d) { super(n); value = d; anim = d ? 1f : 0f; }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { value = e.getAsBoolean(); anim = value ? 1f : 0f; }
    }

    public static final class Num extends Setting {
        public double value; public final double min, max, step;
        public Num(String n, double min, double max, double def, double step) {
            super(n); this.min = min; this.max = max; this.step = step; this.value = def;
        }
        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, v));
        }
        public float f() { return (float) value; }
        public int i() { return (int) Math.round(value); }
        public float frac() { return (float) ((value - min) / (max - min)); }
        public String text() { return step >= 1 ? String.valueOf(i()) : String.format("%.1f", value); }
        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { set(e.getAsDouble()); }
    }

    public static final class Mode extends Setting {
        public final String[] modes; public int index;
        public Mode(String n, int def, String... modes) { super(n); this.modes = modes; this.index = def; }
        public String get() { return modes[index]; }
        public void cycle(int d) { index = Math.floorMod(index + d, modes.length); onChange.run(); }
        public JsonElement save() { return new JsonPrimitive(index); }
        public void load(JsonElement e) { index = Math.floorMod(e.getAsInt(), modes.length); }
    }

    public static final class Color extends Setting {
        public int rgb;
        public Color(String n, int rgb) { super(n); this.rgb = rgb; }
        public JsonElement save() { return new JsonPrimitive(rgb); }
        public void load(JsonElement e) { rgb = e.getAsInt() & 0xFFFFFF; }
        public int ch(int i) { return (rgb >> (16 - 8 * i)) & 255; }
        public void setCh(int i, int v) {
            int[] c = { ch(0), ch(1), ch(2) };
            c[i] = Math.max(0, Math.min(255, v));
            rgb = (c[0] << 16) | (c[1] << 8) | c[2];
        }
    }

    // ---------- module ----------
    public final String name, description;
    public final Category category;
    public boolean enabled;
    public int key = -1;
    public float anim;
    public boolean keyDown;
    public final List<Setting> settings = new ArrayList<>();
    public final Bool holdMode = new Bool("Hold To Enable", false);
    public final Bool showInList = new Bool("Show In Module List", true);

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    protected Bool bool(String n, boolean d) { Bool b = new Bool(n, d); settings.add(b); return b; }
    protected Num num(String n, double min, double max, double def, double step) { Num x = new Num(n, min, max, def, step); settings.add(x); return x; }
    protected Mode mode(String n, int def, String... m) { Mode x = new Mode(n, def, m); settings.add(x); return x; }
    protected Color color(String n, int rgb) { Color x = new Color(n, rgb); settings.add(x); return x; }

    public List<Setting> allSettings() {
        List<Setting> all = new ArrayList<>(settings);
        all.add(holdMode); all.add(showInList);
        return all;
    }

    /** A locked module cannot be turned off normally (see Watermark). */
    public boolean locked() { return false; }
    private boolean bypass;
    /** Turns a locked module off after the user proved they know the code. */
    public void unlockDisable() { bypass = true; setEnabled(false); bypass = false; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean v) {
        if (enabled == v) return;
        if (!v && locked() && !bypass) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    /** 20 Hz, only while enabled and in a world. */
    public void onTick() {}
    /** every rendered frame, only while enabled. */
    public void onFrame(float dt) {}
    /** every rendered frame, always (for easing back to neutral). */
    public void update(float dt) {}
}

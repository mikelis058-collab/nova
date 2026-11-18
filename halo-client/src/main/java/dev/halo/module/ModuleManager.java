package dev.halo.module;

import dev.halo.Anim;
import dev.halo.gui.ClickGui;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private ModuleManager() {}

    public static final List<Module> ALL = new ArrayList<>();
    public static Modules.Interface INTERFACE;
    public static Modules.Zoom ZOOM;
    public static Modules.Fps FPS;
    public static Modules.CpsDisplay CPS;
    public static Modules.Coords COORDS;
    public static Modules.Keystrokes KEYSTROKES;
    public static Modules.Fog FOG;
    public static Modules.Xray XRAY;
    public static Modules.Hats HATS;
    public static Modules.Knives KNIVES;
    public static Modules.ShieldStatus SHIELD;
    public static Modules.Atmosphere ATMOS;

    public static float dt = 0.016f;
    private static long last = System.nanoTime();
    private static boolean menuDown;

    private static <T extends Module> T reg(T m) { ALL.add(m); return m; }

    public static void init() {
        INTERFACE = reg(new Modules.Interface());
        reg(new Modules.TriggerBot());
        reg(new Modules.AimHelper());
        SHIELD = reg(new Modules.ShieldStatus());
        XRAY = reg(new Modules.Xray());
        FOG = reg(new Modules.Fog());
        ATMOS = reg(new Modules.Atmosphere());
        reg(new Modules.Fullbright());
        ZOOM = reg(new Modules.Zoom());
        KNIVES = reg(new Modules.Knives());
        HATS = reg(new Modules.Hats());
        CPS = reg(new Modules.CpsDisplay());
        FPS = reg(new Modules.Fps());
        COORDS = reg(new Modules.Coords());
        KEYSTROKES = reg(new Modules.Keystrokes());
        reg(new Modules.ToggleSprint());
    }

    public static List<Module> in(Module.Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    /** Client tick: keybinds + module ticks. */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        long h = mc.getWindow().getHandle();

        boolean md = mc.player != null && GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (md && !menuDown && mc.currentScreen == null) mc.setScreen(new ClickGui());
        menuDown = md;

        for (Module m : ALL) {
            if (m.key > 0) {
                if (mc.currentScreen != null) {
                    if (m.keyDown && m.holdMode.value) m.setEnabled(false);
                    m.keyDown = false;
                } else {
                    boolean down = GLFW.glfwGetKey(h, m.key) == GLFW.GLFW_PRESS;
                    if (down != m.keyDown) {
                        if (m.holdMode.value) m.setEnabled(down);
                        else if (down) m.toggle();
                        m.keyDown = down;
                    }
                }
            }
            if (m.enabled && mc.player != null) m.onTick();
        }
        Modules.Knives.sync();
    }

    /** Called every rendered frame (deduplicated, so calling it from several places is fine). */
    public static void frame() {
        long now = System.nanoTime();
        float d = (now - last) / 1_000_000_000f;
        if (d < 0.002f) return;
        last = now;
        dt = Math.min(d, 0.1f);

        MinecraftClient mc = MinecraftClient.getInstance();
        Modules.ClickTracker.update();
        for (Module m : ALL) {
            m.anim = Anim.approach(m.anim, m.enabled ? 1f : 0f, 12f, dt);
            m.update(dt);
            if (m.enabled && mc.player != null && mc.world != null) m.onFrame(dt);
        }
    }
}

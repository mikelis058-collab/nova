package dev.halo.gui;

import dev.halo.Anim;
import dev.halo.Config;
import dev.halo.Gfx;
import dev.halo.module.Module;
import dev.halo.module.Module.*;
import dev.halo.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Dark-glass click GUI. Uses GLFW polling for input so it does not depend on Screen input signatures. */
public class ClickGui extends Screen {
    private float open;
    private Category cat = Category.COMBAT;
    private Module sel;
    private float pillY = -1;
    private float scroll, maxScroll;
    private Module listening;
    private Module codeFor;
    private final StringBuilder code = new StringBuilder();
    private long errUntil;
    private boolean first = true, pDown, pRight, pMenu;
    private Setting dragS;
    private int dragCh;
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final Map<Object, Float> hover = new IdentityHashMap<>();

    // per-frame input state
    private int mx, my;
    private boolean click, rclick, down;
    private float dt, ease;

    public ClickGui() {
        super(Text.literal("Halo"));
        sel = ModuleManager.in(cat).get(0);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return listening == null && codeFor == null; }
    @Override public void removed() { Config.save(); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) vertical * 18f));
        return true;
    }

    private float hv(Object k, boolean target) {
        float v = Anim.approach(hover.getOrDefault(k, 0f), target ? 1f : 0f, 16f, dt);
        hover.put(k, v);
        return v;
    }

    private void pollInput(int mouseX, int mouseY) {
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        boolean d = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean menu = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean fresh = first;
        first = false;
        click = !fresh && d && !pDown;
        rclick = !fresh && r && !pRight;
        down = d;
        if (!d) dragS = null;
        mx = mouseX; my = mouseY;

        if (listening != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                boolean kd = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
                if (kd && !prevKeys[k]) {
                    if (k == GLFW.GLFW_KEY_ESCAPE) { /* cancel */ }
                    else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) listening.key = -1;
                    else listening.key = k;
                    listening = null;
                    for (int j = GLFW.GLFW_KEY_SPACE; j <= GLFW.GLFW_KEY_LAST; j++) prevKeys[j] = GLFW.glfwGetKey(h, j) == GLFW.GLFW_PRESS;
                    break;
                }
                prevKeys[k] = kd;
            }
        } else if (codeFor != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                boolean kd = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
                boolean edge = kd && !prevKeys[k];
                prevKeys[k] = kd;
                if (edge) codeKey(k);
                if (codeFor == null) break;
            }
        } else if (menu && !pMenu && !fresh) {
            close();
        }
        pMenu = menu; pDown = d; pRight = r;
    }

    private void startListening(Module m) {
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        listening = m;
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ModuleManager.frame();
        dt = ModuleManager.dt;
        pollInput(mouseX, mouseY);
        if (codeFor != null) { click = false; rclick = false; }

        open = Anim.approach(open, 1f, 14f, dt);
        float inv = 1f - open;
        ease = 1f - inv * inv * inv;
        int acc = Gfx.accent();

        int sw = width, sh = height;
        int w = Math.min(500, sw - 16), hgt = Math.min(290, sh - 16);
        int x = (sw - w) / 2, y = (sh - hgt) / 2 + (int) ((1f - ease) * 14f);

        ctx.fill(0, 0, sw, sh, Gfx.alpha(0x000000, 0.42f * ease));
        Gfx.glass(ctx, x, y, w, hgt, 10, ease);

        // header
        Gfx.rrect(ctx, x + 14, y + 12, 3, 12, 1, Gfx.alpha(acc, ease));
        Gfx.text(ctx, "H A L O", x + 23, y + 14, Gfx.alpha(0xFFFFFF, ease));
        String hint = "RSHIFT to close";
        Gfx.text(ctx, hint, x + w - 14 - Gfx.width(hint), y + 14, Gfx.alpha(0x808080, ease));
        ctx.fill(x + 12, y + 31, x + w - 12, y + 32, Gfx.alpha(0xFFFFFF, 0.07f * ease));

        drawCategories(ctx, x, y, acc);
        drawModules(ctx, x, y, hgt, acc);
        drawSettings(ctx, x, y, w, hgt, acc);
        drawCodePrompt(ctx, sw, sh);
    }

    private void openCode(Module m) {
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        codeFor = m; code.setLength(0);
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) prevKeys[k] = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
    }

    private void codeKey(int k) {
        if (k == GLFW.GLFW_KEY_ESCAPE) { codeFor = null; code.setLength(0); return; }
        if (k == GLFW.GLFW_KEY_BACKSPACE) { if (code.length() > 0) code.setLength(code.length() - 1); return; }
        int digit = -1;
        if (k >= GLFW.GLFW_KEY_0 && k <= GLFW.GLFW_KEY_9) digit = k - GLFW.GLFW_KEY_0;
        else if (k >= GLFW.GLFW_KEY_KP_0 && k <= GLFW.GLFW_KEY_KP_9) digit = k - GLFW.GLFW_KEY_KP_0;
        if (digit < 0 || System.currentTimeMillis() < errUntil) return;
        code.append(digit);
        if (code.length() == 4) {
            if (code.toString().equals("4321")) { Module m = codeFor; codeFor = null; code.setLength(0); m.unlockDisable(); }
            else { errUntil = System.currentTimeMillis() + 700; code.setLength(0); }
        }
    }

    private void drawCodePrompt(DrawContext ctx, int sw, int sh) {
        if (codeFor == null) return;
        boolean err = System.currentTimeMillis() < errUntil;
        ctx.fill(0, 0, sw, sh, Gfx.alpha(0x000000, 0.55f));
        int mw = 210, mh = 92, x = (sw - mw) / 2, y = (sh - mh) / 2;
        Gfx.glass(ctx, x, y, mw, mh, 10, 1f);
        Gfx.centered(ctx, "Enter code to turn off", x + mw / 2, y + 12, Gfx.alpha(0xFFFFFF, 1f));
        for (int i = 0; i < 4; i++) {
            int bx = x + (mw - 128) / 2 + i * 34, by = y + 30;
            Gfx.rrect(ctx, bx, by, 26, 28, 6, err ? Gfx.alpha(0xFF4444, 0.30f) : Gfx.alpha(0xFFFFFF, 0.10f));
            if (i < code.length()) Gfx.rrect(ctx, bx + 9, by + 10, 8, 8, 4, Gfx.alpha(0xFFFFFF, 1f));
        }
        String msg = err ? "Wrong code" : "Backspace erase  |  Esc cancel";
        Gfx.centered(ctx, msg, x + mw / 2, y + 68, Gfx.alpha(err ? 0xFF5555 : 0x808080, 1f));
    }

    // ------------------------------------------------------------ categories
    private void drawCategories(DrawContext ctx, int x, int y, int acc) {
        Category[] cats = Category.values();
        int bx = x + 10, by = y + 40;
        int target = by + cat.ordinal() * 24;
        pillY = pillY < 0 ? target : Anim.approach(pillY, target, 18f, dt);
        Gfx.rrect(ctx, bx, Math.round(pillY), 82, 20, 6, Gfx.alpha(acc, 0.20f * ease));
        Gfx.rrect(ctx, bx + 3, Math.round(pillY) + 5, 2, 10, 1, Gfx.alpha(acc, ease));
        for (int i = 0; i < cats.length; i++) {
            int ry = by + i * 24;
            boolean hov = Gfx.inside(mx, my, bx, ry, 82, 20);
            float hh = hv(cats[i], hov || cats[i] == cat);
            Gfx.text(ctx, cats[i].label, bx + 12, ry + 6, Gfx.alpha(Gfx.mix(0x909090, 0xFFFFFF, hh), ease));
            if (click && hov && cats[i] != cat) {
                cat = cats[i];
                sel = ModuleManager.in(cat).get(0);
                scroll = 0;
            }
        }
    }

    // ------------------------------------------------------------ module list
    private void drawModules(DrawContext ctx, int x, int y, int hgt, int acc) {
        int lx = x + 100, lw = 172, ly = y + 40;
        for (Module m : ModuleManager.in(cat)) {
            boolean hov = Gfx.inside(mx, my, lx, ly, lw, 24);
            float hh = hv(m, hov || m == sel);
            Gfx.rrect(ctx, lx, ly, lw, 24, 6, Gfx.alpha(0xFFFFFF, ease * (0.035f + 0.05f * hh)));
            if (m == sel) {
                Gfx.rrect(ctx, lx, ly, lw, 24, 6, Gfx.alpha(acc, ease * 0.14f));
                Gfx.rrect(ctx, lx + 3, ly + 6, 2, 12, 1, Gfx.alpha(acc, ease));
            }
            Gfx.text(ctx, m.name, lx + 12, ly + 8, Gfx.alpha(Gfx.mix(0xB4B4B4, 0xFFFFFF, hh), ease));
            if (m.key > 0) {
                String kn = keyName(m.key);
                Gfx.text(ctx, kn, lx + lw - 42 - Gfx.width(kn), ly + 8, Gfx.alpha(0x6E6E6E, ease));
            }
            int sx = lx + lw - 34, sy = ly + 6;
            drawSwitch(ctx, sx, sy, m.anim, acc);
            if (click && hov) {
                if (Gfx.inside(mx, my, sx - 3, sy - 3, 32, 18)) { if (m.enabled && m.locked()) openCode(m); else m.toggle(); }
                else { sel = m; scroll = 0; }
            }
            ly += 27;
        }
    }

    private void drawSwitch(DrawContext ctx, int x, int y, float a, int acc) {
        Gfx.rrect(ctx, x, y, 26, 12, 6, Gfx.alpha(Gfx.mix(0x3A3A3A, acc, a), ease));
        Gfx.rrect(ctx, x + 2 + Math.round(14 * a), y + 2, 8, 8, 4, Gfx.alpha(Gfx.mix(0xFFFFFF, Gfx.onAccent(acc), a), ease));
    }

    // ------------------------------------------------------------ settings
    private void drawSettings(DrawContext ctx, int x, int y, int w, int hgt, int acc) {
        int px = x + 284, pw = w - 284 - 14;
        Gfx.rrect(ctx, x + 278, y + 40, 1, hgt - 52, 0, Gfx.alpha(0xFFFFFF, 0.06f * ease));
        if (sel == null) return;

        Gfx.text(ctx, sel.name, px, y + 40, Gfx.alpha(0xFFFFFF, ease));
        String desc = sel.description;
        while (Gfx.width(desc) > pw && desc.length() > 4) desc = desc.substring(0, desc.length() - 4) + "..";
        Gfx.text(ctx, desc, px, y + 52, Gfx.alpha(0x808080, ease));

        // bind button
        int by = y + 68;
        Gfx.text(ctx, "Keybind", px, by + 5, Gfx.alpha(0xB4B4B4, ease));
        String bt = listening == sel ? "press a key..." : (sel.key > 0 ? keyName(sel.key) : "None");
        int bw = Math.max(64, Gfx.width(bt) + 16), bx = px + pw - bw;
        boolean bh = Gfx.inside(mx, my, bx, by, bw, 18);
        float bhv = hv("bind", bh || listening == sel);
        Gfx.rrect(ctx, bx, by, bw, 18, 5, Gfx.alpha(listening == sel ? acc : 0xFFFFFF, ease * (listening == sel ? 0.3f : 0.06f + 0.06f * bhv)));
        Gfx.centered(ctx, bt, bx + bw / 2, by + 5, Gfx.alpha(0xFFFFFF, ease));
        if (click && bh) { if (listening == sel) listening = null; else startListening(sel); }
        if (rclick && bh) sel.key = -1;

        // scroll region
        int top = y + 94, bottom = y + hgt - 12;
        ctx.enableScissor(px - 4, top, px + pw + 4, bottom);
        int cy = top - Math.round(scroll);
        int start = cy;
        List<Setting> list = sel.allSettings();
        for (Setting s : list) {
            if (s == sel.holdMode && !sel.settings.isEmpty()) {
                Gfx.text(ctx, "GENERAL", px, cy + 2, Gfx.alpha(0x5A5A5A, ease));
                cy += 14;
            }
            cy += drawSetting(ctx, s, px, cy, pw, top, bottom, acc);
        }
        ctx.disableScissor();
        maxScroll = Math.max(0, (cy - start) - (bottom - top));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    private boolean live(int top, int bottom) { return my >= top && my < bottom; }

    private int drawSetting(DrawContext ctx, Setting s, int px, int cy, int pw, int top, int bottom, int acc) {
        if (s instanceof Bool b) {
            b.anim = Anim.approach(b.anim, b.value ? 1f : 0f, 14f, dt);
            Gfx.text(ctx, b.name, px, cy + 4, Gfx.alpha(0xD2D2D2, ease));
            drawSwitch(ctx, px + pw - 28, cy + 2, b.anim, acc);
            if (click && live(top, bottom) && Gfx.inside(mx, my, px, cy, pw, 18)) { b.value = !b.value; b.onChange.run(); }
            return 22;
        }
        if (s instanceof Mode m) {
            Gfx.text(ctx, m.name, px, cy + 5, Gfx.alpha(0xD2D2D2, ease));
            String t = "<  " + m.get() + "  >";
            int tw = Gfx.width(t) + 12, bx = px + pw - tw;
            boolean hov = Gfx.inside(mx, my, bx, cy, tw, 18) && live(top, bottom);
            float hh = hv(m, hov);
            Gfx.rrect(ctx, bx, cy, tw, 18, 5, Gfx.alpha(0xFFFFFF, ease * (0.06f + 0.06f * hh)));
            Gfx.centered(ctx, t, bx + tw / 2, cy + 5, Gfx.alpha(0xFFFFFF, ease));
            if (hov && click) m.cycle(1);
            if (hov && rclick) m.cycle(-1);
            return 24;
        }
        if (s instanceof Num n) {
            Gfx.text(ctx, n.name, px, cy + 1, Gfx.alpha(0xD2D2D2, ease));
            Gfx.text(ctx, n.text(), px + pw - Gfx.width(n.text()), cy + 1, Gfx.alpha(0xFFFFFF, ease));
            slider(ctx, n, -1, px, cy + 14, pw, top, bottom, acc, n.frac());
            return 28;
        }
        if (s instanceof Module.Color c) {
            Gfx.text(ctx, c.name, px, cy + 1, Gfx.alpha(0xD2D2D2, ease));
            Gfx.rrect(ctx, px + pw - 24, cy, 24, 10, 3, Gfx.alpha(c.rgb, ease));
            int[] cols = { 0xFF5566, 0x55DD77, 0x5599FF };
            for (int i = 0; i < 3; i++) slider(ctx, c, i, px, cy + 16 + i * 11, pw, top, bottom, cols[i], c.ch(i) / 255f);
            return 56;
        }
        return 0;
    }

    private void slider(DrawContext ctx, Setting s, int ch, int px, int ty, int pw, int top, int bottom, int acc, float frac) {
        boolean hov = Gfx.inside(mx, my, px - 2, ty - 4, pw + 4, 12) && live(top, bottom);
        if (click && hov) { dragS = s; dragCh = ch; }
        if (dragS == s && dragCh == ch && down) {
            float f = Math.max(0f, Math.min(1f, (mx - px) / (float) pw));
            if (s instanceof Num n) { n.set(n.min + f * (n.max - n.min)); n.onChange.run(); frac = n.frac(); }
            else if (s instanceof Module.Color c) { c.setCh(ch, Math.round(f * 255f)); frac = f; }
        }
        float hh = hv(s, hov || (dragS == s && dragCh == ch));
        Gfx.rrect(ctx, px, ty, pw, 4, 2, Gfx.alpha(0xFFFFFF, ease * 0.10f));
        int fw = Math.max(4, Math.round(pw * frac));
        Gfx.rrect(ctx, px, ty, fw, 4, 2, Gfx.alpha(acc, ease));
        int kr = 3 + Math.round(hh * 1.5f);
        Gfx.rrect(ctx, px + fw - kr, ty + 2 - kr, kr * 2, kr * 2, kr, Gfx.alpha(0xFFFFFF, ease));
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) ? "F" + (key - GLFW.GLFW_KEY_F1 + 1) : "KEY " + key;
        };
    }
}

package dev.halo;

import dev.halo.module.Module;
import dev.halo.module.ModuleManager;
import dev.halo.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Black-and-white glass HUD. Everything eases in/out and widths glide instead of jumping. */
public final class Hud {
    private Hud() {}

    private static final int GRAY = 0x8A8A8A;
    private static final float[] KEY_ANIM = new float[8];
    private static final Map<Object, Float> WIDTH = new IdentityHashMap<>();
    private static float fpsShown = 60f;

    public static void render(DrawContext ctx) {
        ModuleManager.frame();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        var ui = ModuleManager.INTERFACE;
        if (ui.anim < 0.01f) return;

        float ia = Anim.ease(ui.anim);
        float dt = ModuleManager.dt;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int acc = Gfx.accent();

        fpsShown = Anim.approach(fpsShown, mc.getCurrentFps(), 5f, dt);

        int y = 6;
        y = widget(ctx, ModuleManager.FPS, "FPS", String.valueOf(Math.round(fpsShown)), y, ia, acc);

        String cps = Modules.ClickTracker.left() + (ModuleManager.CPS.right.value ? " | " + Modules.ClickTracker.right() : "");
        y = widget(ctx, ModuleManager.CPS, "CPS", cps, y, ia, acc);

        int d = ModuleManager.COORDS.decimals.i();
        String fmt = "%." + d + "f";
        String pos = String.format(fmt + "  " + fmt + "  " + fmt, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        String label = "XYZ";
        if (ModuleManager.COORDS.direction.value) {
            String f = mc.player.getHorizontalFacing().asString();
            label = Character.toUpperCase(f.charAt(0)) + f.substring(1);
        }
        widget(ctx, ModuleManager.COORDS, label, pos, y, ia, acc);

        if (ui.moduleList.value) moduleList(ctx, sw, ia, acc);
        keystrokes(ctx, sh, ia, acc);
    }

    /** Info pill: white value, gray label, accent bar. Width glides to fit the text. */
    private static int widget(DrawContext ctx, Module m, String label, String value, int y, float ia, int acc) {
        if (m.anim < 0.01f) return y;
        float e = Anim.ease(m.anim);
        float a = e * ia;
        int target = Gfx.width(value) + Gfx.width(label) + 26;
        float w = Anim.approach(WIDTH.getOrDefault(m, (float) target), target, 16f, ModuleManager.dt);
        WIDTH.put(m, w);
        int iw = Math.round(w);
        int x = 6 - Math.round((1f - e) * 26f);

        Gfx.glass(ctx, x, y, iw, 17, 5, a);
        Gfx.rrect(ctx, x + 5, y + 5, 2, 7, 1, Gfx.alpha(acc, a));
        Gfx.text(ctx, value, x + 12, y + 5, Gfx.alpha(0xFFFFFF, a));
        Gfx.text(ctx, label, x + 12 + Gfx.width(value) + 5, y + 5, Gfx.alpha(GRAY, a));
        return y + Math.round(21 * e);
    }

    private static void moduleList(DrawContext ctx, int sw, float ia, int acc) {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.ALL)
            if (m.showInList.value && m.anim > 0.02f) list.add(m);
        list.sort((p, q) -> Gfx.width(q.name) - Gfx.width(p.name));
        int y = 6;
        for (Module m : list) {
            float e = Anim.ease(m.anim);
            float a = e * ia;
            int w = Gfx.width(m.name) + 18;
            int x = sw - 6 - w + Math.round((1f - e) * (w + 12));
            Gfx.glass(ctx, x, y, w, 17, 5, a);
            Gfx.rrect(ctx, x + w - 8, y + 5, 2, 7, 1, Gfx.alpha(acc, a));
            Gfx.text(ctx, m.name, x + 8, y + 5, Gfx.alpha(0xFFFFFF, a));
            y += Math.round(21 * e);
        }
    }

    private static void keystrokes(DrawContext ctx, int sh, float ia, int acc) {
        var k = ModuleManager.KEYSTROKES;
        if (k.anim < 0.01f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float e = Anim.ease(k.anim);
        float a = e * ia;
        float dt = ModuleManager.dt;
        int x = 6 - Math.round((1f - e) * 40f), y = sh / 2 - 30;
        var o = mc.options;
        boolean[] down = { o.forwardKey.isPressed(), o.leftKey.isPressed(), o.backKey.isPressed(), o.rightKey.isPressed(),
                Modules.ClickTracker.leftDown, Modules.ClickTracker.rightDown, o.jumpKey.isPressed() };
        for (int i = 0; i < down.length; i++) KEY_ANIM[i] = Anim.approach(KEY_ANIM[i], down[i] ? 1f : 0f, 24f, dt);

        keyBox(ctx, x + 23, y, 21, 21, "W", KEY_ANIM[0], a, acc);
        keyBox(ctx, x, y + 23, 21, 21, "A", KEY_ANIM[1], a, acc);
        keyBox(ctx, x + 23, y + 23, 21, 21, "S", KEY_ANIM[2], a, acc);
        keyBox(ctx, x + 46, y + 23, 21, 21, "D", KEY_ANIM[3], a, acc);
        int yy = y + 46;
        if (k.mouse.value) {
            keyBox(ctx, x, yy, 32, 21, "L " + Modules.ClickTracker.left(), KEY_ANIM[4], a, acc);
            keyBox(ctx, x + 35, yy, 32, 21, "R " + Modules.ClickTracker.right(), KEY_ANIM[5], a, acc);
            yy += 23;
        }
        if (k.space.value) keyBox(ctx, x, yy, 67, 14, "SPACE", KEY_ANIM[6], a, acc);
    }

    /** Pressed keys smoothly invert: glass turns solid white and the text turns black. */
    private static void keyBox(DrawContext ctx, int x, int y, int w, int h, String label, float press, float a, int acc) {
        Gfx.glass(ctx, x, y, w, h, 5, a);
        if (press > 0.01f) Gfx.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 4, Gfx.alpha(acc, a * 0.94f * press));
        Gfx.centered(ctx, label, x + w / 2, y + (h - 8) / 2, Gfx.alpha(Gfx.mix(0xB4B4B4, Gfx.onAccent(acc), press), a));
    }
}

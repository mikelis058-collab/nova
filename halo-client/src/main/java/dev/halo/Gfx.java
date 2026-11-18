package dev.halo;

import dev.halo.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Gfx {
    private Gfx() {}

    public static int alpha(int rgb, float a) {
        int al = (int) (Math.max(0f, Math.min(1f, a)) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static int accent() {
        return ModuleManager.INTERFACE == null ? 0x8C78FF : ModuleManager.INTERFACE.accent.rgb;
    }

    /** Text/knob color that stays readable on top of the accent color. */
    public static int onAccent(int acc) {
        double lum = 0.299 * ((acc >> 16) & 255) + 0.587 * ((acc >> 8) & 255) + 0.114 * (acc & 255);
        return lum > 150 ? 0x101010 : 0xFFFFFF;
    }

    public static float opacity() {
        return ModuleManager.INTERFACE == null ? 0.8f : ModuleManager.INTERFACE.opacity.f() / 100f;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Rounded rectangle drawn with scanlines. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, col);
        }
    }

    /** Dark glass panel: soft shadow, thin edge, translucent body, top gloss. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, float a) {
        if (a <= 0.01f) return;
        float op = opacity();
        for (int i = 3; i >= 1; i--) rrect(c, x - i, y - i + 1, w + 2 * i, h + 2 * i, r + i, alpha(0x000000, a * 0.07f));
        rrect(c, x, y, w, h, r, alpha(0xFFFFFF, a * 0.17f));
        rrect(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), alpha(0x050505, a * op));
        c.fillGradient(x + r, y + 1, x + w - r, y + Math.max(3, h / 2), alpha(0xFFFFFF, a * 0.085f), alpha(0xFFFFFF, 0f));
        c.fillGradient(x + r, y + h / 2, x + w - r, y + h - 1, alpha(0x000000, 0f), alpha(0x000000, a * 0.30f));
        c.fill(x + r, y + 1, x + w - r, y + 2, alpha(0xFFFFFF, a * 0.16f));
    }

    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext c, String s, int x, int y, int color) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, false);
    }

    public static void centered(DrawContext c, String s, int cx, int y, int color) {
        text(c, s, cx - width(s) / 2, y, color);
    }
}

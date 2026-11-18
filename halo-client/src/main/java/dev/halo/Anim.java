package dev.halo;

public final class Anim {
    private Anim() {}
    /** Cubic ease-out for 0..1 values. */
    public static float ease(float t) { float i = 1f - Math.max(0f, Math.min(1f, t)); return 1f - i * i * i; }

    /** Frame-rate independent exponential smoothing. */
    public static float approach(float cur, float target, float speed, float dt) {
        float v = cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(v - target) < 0.002f ? target : v;
    }
}

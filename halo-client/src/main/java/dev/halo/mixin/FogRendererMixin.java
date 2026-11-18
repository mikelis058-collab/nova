package dev.halo.mixin;

import dev.halo.Gfx;
import dev.halo.module.ModuleManager;
import dev.halo.module.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only terrain fog is touched (renderDistanceStart/End) and the fog/horizon color.
 * skyEnd / cloudEnd are left alone, so clouds keep their normal look.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    private static final String APPLY = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V";

    private static Vector4f towards(Vector4f o, int rgb, float t) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        return new Vector4f(o.x + (r - o.x) * t, o.y + (g - o.y) * t, o.z + (b - o.z) * t, o.w);
    }

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void halo$color(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f o = cir.getReturnValue();
        boolean changed = false;

        float ab = Modules.Atmosphere.blend();
        var at = ModuleManager.ATMOS;
        if (ab > 0f && at.tintHorizon.value) {
            o = towards(o, at.horizon.rgb, at.strength.f() / 100f * ab);
            changed = true;
        }
        float b = Modules.Fog.blend();
        if (b > 0f) {
            var f = ModuleManager.FOG;
            o = towards(o, f.color.rgb, f.tint.f() / 100f * b);
            changed = true;
        }
        if (changed) cir.setReturnValue(o);
    }

    @ModifyVariable(method = APPLY, at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float halo$start(float start) {
        float b = Modules.Fog.blend();
        return b <= 0f ? start : start * Modules.Fog.startScale(b);
    }

    @ModifyVariable(method = APPLY, at = @At("HEAD"), argsOnly = true, ordinal = 3, require = 0)
    private float halo$end(float end) {
        float b = Modules.Fog.blend();
        return b <= 0f ? end : end * Modules.Fog.endScale(b);
    }
}

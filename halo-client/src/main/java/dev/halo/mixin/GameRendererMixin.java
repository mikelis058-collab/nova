package dev.halo.mixin;

import dev.halo.module.ModuleManager;
import dev.halo.module.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /** Zoom: shrink the field of view. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void halo$zoom(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float z = Modules.zoomFactor();
        if (z > 1.001f) {
            double half = Math.toRadians(cir.getReturnValue()) / 2.0;
            cir.setReturnValue((float) Math.toDegrees(2.0 * Math.atan(Math.tan(half) / z)));
        }
    }

    /** Per-frame hook for smooth aiming, TriggerBot timing and animations. */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void halo$frame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ModuleManager.frame();
    }
}

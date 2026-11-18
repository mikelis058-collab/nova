package dev.halo.mixin;

import dev.halo.module.Modules;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    /** Atmosphere: tint the main sky dome color. */
    @ModifyVariable(method = "renderTopSky", at = @At("HEAD"), argsOnly = true, require = 0)
    private int halo$sky(int color) {
        return Modules.Atmosphere.tintSky(color);
    }
}

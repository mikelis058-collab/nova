package dev.halo.mixin;

import dev.halo.module.Modules;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /** Status 30 = a shield was knocked out (axe hit). Used by ShieldStatus. */
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void halo$status(byte status, CallbackInfo ci) {
        if (status == 30) Modules.ShieldStatus.onBreak((LivingEntity) (Object) this);
    }
}

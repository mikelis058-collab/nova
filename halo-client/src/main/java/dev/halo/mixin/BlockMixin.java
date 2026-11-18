package dev.halo.mixin;

import dev.halo.module.Modules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    /** Xray: only draw faces of "interesting" blocks, and draw all of their faces. */
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true, require = 0)
    private static void halo$xray(BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.Xray.active()) cir.setReturnValue(Modules.Xray.visible(state));
    }
}

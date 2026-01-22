package com.kreidev.cmverticaladditions.mixin;

import com.kreidev.cmverticaladditions.VerticalBeltBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.simibubi.create.content.kinetics.belt.BeltBlock.SLOPE;

@Mixin(value = BeltBlock.class, remap = false)
public class BeltBlockMixin {

    @Inject(method = "canTransportObjects", at = @At(value = "HEAD"), cancellable = true)
    private static void canTransportObjects(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!AllBlocks.BELT.has(state)) return;

        if (state.getBlock() instanceof VerticalBeltBlock && state.getValue(SLOPE) == BeltSlope.VERTICAL) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}

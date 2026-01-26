package com.kreidev.cmverticaladditions.mixin;

import com.kreidev.cmverticaladditions.VerticalAdditions;
import com.kreidev.cmverticaladditions.VerticalBeltBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
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
        if (state.getBlock() instanceof VerticalBeltBlock) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @WrapOperation(
            method = "initBelt",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean initBeltHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
        VerticalAdditions.LOGGER.debug("initBelt: " + state);
        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
    }

    @WrapOperation(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean onRemoveHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
        VerticalAdditions.LOGGER.debug("onRemove: " + state);
        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
    }

    @WrapOperation(
            method = "getBeltChain",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean getBeltChainHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
        VerticalAdditions.LOGGER.debug("getBeltChain: " + state);
        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
    }
}

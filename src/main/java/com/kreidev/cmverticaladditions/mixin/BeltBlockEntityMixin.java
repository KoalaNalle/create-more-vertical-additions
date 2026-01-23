package com.kreidev.cmverticaladditions.mixin;

import com.kreidev.cmverticaladditions.VerticalAdditions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BeltBlockEntity.class, remap = false)
public class BeltBlockEntityMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean tickHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
    }

    @WrapOperation(
            method = "hasPulley",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private static boolean hasPulleyHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
    }
}

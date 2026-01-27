package com.kreidev.cmverticaladditions.mixin;

import com.kreidev.cmverticaladditions.VerticalAdditions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BeltRenderer.class, remap = false)
public class BeltRendererMixin {

//    @WrapOperation(
//            method = "renderSafe",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"
//            )
//    )
//    private static boolean renderSafeHas(BlockEntry<Block> instance, BlockState state, Operation<Boolean> original) {
////        VerticalAdditions.LOGGER.debug("renderSafe: " + state);
////        return original.call(instance, state) || state.getBlock() instanceof BeltBlock;
//    }
}

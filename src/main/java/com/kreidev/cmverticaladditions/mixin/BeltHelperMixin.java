package com.kreidev.cmverticaladditions.mixin;

import com.kreidev.cmverticaladditions.VerticalBeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeltHelper.class, remap = false)
public class BeltHelperMixin {

    // NOTE: still running on the assumption that controller is at BeltPart.START and is always at the bottom end.

    @Inject(method = "getPositionForOffset", at = @At(value = "HEAD"), cancellable = true)
    private static void getPositionForOffset(BeltBlockEntity controller, int offset, CallbackInfoReturnable<BlockPos> cir) {
        if (controller instanceof VerticalBeltBlockEntity) {
            Direction facing = controller.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos pos = controller.getBlockPos();

            if (offset == -1) {
                cir.setReturnValue(pos.relative(facing.getOpposite()));
                cir.cancel();
            } else if (offset < controller.beltLength) {
                cir.setReturnValue(pos.above(offset-1));
                cir.cancel();
            } else {
                cir.setReturnValue(pos.above(controller.beltLength-1).relative(facing));
                cir.cancel();
            }
        }
    }

    @Inject(method = "getVectorForOffset", at = @At(value = "HEAD"), cancellable = true)
    private static void getVectorForOffset(BeltBlockEntity controller, float offset, CallbackInfoReturnable<Vec3> cir) {
        if (controller instanceof VerticalBeltBlockEntity) {
            if (offset >= controller.beltLength) {
                offset--;  // TODO: Not sure if safe, need to add a check if invoked from BeltInventory.eject
            }
            // TODO: ejection motion doesn't seem to match with horizontal belts

            Vec3 vec = VecHelper.getCenterOf(controller.getBlockPos());
            vec = vec.add(0,offset,0);
            vec = vec.add(Vec3.atLowerCornerOf(controller.getMovementFacing().getNormal()).scale(0.5));

            cir.setReturnValue(vec);
            cir.cancel();
        }
    }
}

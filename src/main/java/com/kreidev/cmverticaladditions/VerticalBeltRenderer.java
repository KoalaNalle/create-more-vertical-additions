package com.kreidev.cmverticaladditions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.*;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

import static com.kreidev.cmverticaladditions.VerticalAdditions.resLoc;

public class VerticalBeltRenderer extends BeltRenderer {

    public static final PartialModel BELT_VERTICAL_START = PartialModel.of(resLoc("block/vertical_start"));
    public static final PartialModel BELT_VERTICAL_START_OFFSET = PartialModel.of(resLoc("block/vertical_start_offset"));

    public VerticalBeltRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        // TODO: figure out flywheel so I don't have to do cpu rendering

        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof VerticalBeltBlock)) return;
        if (blockState.getValue(BeltBlock.SLOPE) != BeltSlope.VERTICAL) return;

        BeltPart part = blockState.getValue(BeltBlock.PART);
        Direction facing = blockState.getValue(BeltBlock.HORIZONTAL_FACING);
        Direction.AxisDirection axisDirection = facing.getAxisDirection();

        boolean start = part == BeltPart.START;
        boolean end = part == BeltPart.END;
        boolean alongX = facing.getAxis() == Direction.Axis.X;

        PoseStack localTransforms = new PoseStack();
        var msr = TransformStack.of(localTransforms);
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        float renderTick = AnimationTickHolder.getRenderTime(be.getLevel());

        if (part == BeltPart.START) {
            msr.center()
                    .rotateYDegrees(AngleHelper.horizontalAngle(facing) + 180)
                    .uncenter();
        } else if (part == BeltPart.END) {
            msr.center()
                    .rotateZDegrees(180)
                    .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                    .uncenter();
        } else {
            msr.center()
                    .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                    .rotateXDegrees(90)
                    .uncenter();
        }

        DyeColor color = be.color.orElse(null);

        for (boolean bottom : Iterate.trueAndFalse) {
            PartialModel beltPartial = getBeltPartial(false, start, end, bottom);

            if (part == BeltPart.START || part == BeltPart.END) {
                beltPartial = bottom ? BELT_VERTICAL_START_OFFSET : BELT_VERTICAL_START;
            }

            SuperByteBuffer beltBuffer = CachedBuffers.partial(beltPartial, blockState)
                    .light(light);

            SpriteShiftEntry spriteShift = getSpriteShiftEntry(color, false, bottom);

            // UV shift
            float speed = be.getSpeed();
            if (speed != 0 || be.color.isPresent()) {
                float time = renderTick * axisDirection.getStep();
                if (alongX) {
                    speed = -speed;
                }

                float scrollMult = 0.5f;

                float spriteSize = spriteShift.getTarget()
                        .getV1()
                        - spriteShift.getTarget()
                        .getV0();

                boolean shouldOffset = bottom ^ part == BeltPart.END;
                double scroll = speed * time / (31.5 * 16) + (shouldOffset ? 0.5 : 0.0) + (part == BeltPart.START || part == BeltPart.END ? 0 : 1/8f);
                scroll = scroll - Math.floor(scroll);
                scroll = scroll * spriteSize * scrollMult;

                beltBuffer.shiftUVScrolling(spriteShift, (float) scroll);
            }

            beltBuffer
                    .transform(localTransforms)
                    .renderInto(ms, vb);
        }

        if (be.hasPulley()) {
            Direction dir = blockState.getValue(BeltBlock.HORIZONTAL_FACING).getClockWise();

            Supplier<PoseStack> matrixStackSupplier = () -> {
                PoseStack stack = new PoseStack();
                var stacker = TransformStack.of(stack);
                stacker.center();
                if (dir.getAxis() == Direction.Axis.X) stacker.rotateYDegrees(90);
                if (dir.getAxis() == Direction.Axis.Y) stacker.rotateXDegrees(90);
                stacker.rotateXDegrees(90);
                stacker.uncenter();
                return stack;
            };

            SuperByteBuffer superBuffer = CachedBuffers.partialDirectional(AllPartialModels.BELT_PULLEY,
                    blockState, dir, matrixStackSupplier);
            KineticBlockEntityRenderer.standardKineticRotationTransform(superBuffer, be, light)
                    .renderInto(ms, vb);
        }

        renderItems(be, partialTicks, ms, buffer, light, overlay);
    }

    public static void init() {}
}

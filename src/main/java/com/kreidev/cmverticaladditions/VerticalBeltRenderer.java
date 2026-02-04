package com.kreidev.cmverticaladditions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.*;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.render.ShadowRenderHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static com.kreidev.cmverticaladditions.VerticalAdditions.resLoc;

public class VerticalBeltRenderer extends BeltRenderer {

    public static final PartialModel STICKY_BELT_VERTICAL_START = PartialModel.of(resLoc("block/sticky_vertical_start"));
    public static final PartialModel STICKY_BELT_VERTICAL_START_OFFSET = PartialModel.of(resLoc("block/sticky_vertical_start_offset"));
    public static final PartialModel STICKY_BELT_MIDDLE = PartialModel.of(resLoc("block/sticky_middle"));
    public static final PartialModel STICKY_BELT_MIDDLE_OFFSET = PartialModel.of(resLoc("block/sticky_middle_bottom"));

    public static final PartialModel BELT_VERTICAL_START = PartialModel.of(resLoc("block/vertical_start"));
    public static final PartialModel BELT_VERTICAL_START_OFFSET = PartialModel.of(resLoc("block/vertical_start_offset"));

    public static final SpriteShiftEntry STICKY_BELT = SpriteShifter.get(resLoc("block/sticky_belt"), resLoc("block/sticky_belt_scroll"));
    public static final SpriteShiftEntry STICKY_BELT_OFFSET = SpriteShifter.get(resLoc("block/sticky_belt_offset"), resLoc("block/sticky_belt_scroll"));

    public static final Map<DyeColor, SpriteShiftEntry> STICKY_DYED_BELTS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, SpriteShiftEntry> STICKY_DYED_OFFSET_BELTS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String id = color.getSerializedName();
            STICKY_DYED_BELTS.put(color, SpriteShifter.get(resLoc("block/sticky_belt"), resLoc("block/sticky_belt/" + id + "_scroll")));
            STICKY_DYED_OFFSET_BELTS.put(color,SpriteShifter.get(resLoc("block/sticky_belt_offset"), resLoc("block/sticky_belt/" + id + "_scroll")));
        }
    }

    public VerticalBeltRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        // TODO: figure out flywheel

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
                    .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                    .rotateZDegrees(180)
                    .uncenter();
        } else {
            msr.center()
                    .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                    .rotateXDegrees(90)
                    .uncenter();
        }

        DyeColor color = be.color.orElse(null);

        for (boolean bottom : Iterate.trueAndFalse) {
            PartialModel beltPartial = getVerticalBeltPartial( start, end, bottom, ClientConfig.stickyTextures);

            SuperByteBuffer beltBuffer = CachedBuffers.partial(beltPartial, blockState)
                    .light(light);

            SpriteShiftEntry spriteShift = getVerticalSpriteShiftEntry(color, bottom, ClientConfig.stickyTextures);

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

    @Override
    protected void renderItems(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                               int light, int overlay) {
        if (!be.isController())
            return;
        if (be.beltLength == 0)
            return;

        ms.pushPose();

        Direction beltFacing = be.getBlockState().getValue(BeltBlock.HORIZONTAL_FACING);
        Vec3i directionVec = beltFacing.getNormal();
        Vec3 beltStartOffset = Vec3.atLowerCornerOf(directionVec)
                .scale(-.5)
                .add(.5, 15 / 16f, .5);
        ms.translate(beltStartOffset.x, beltStartOffset.y, beltStartOffset.z);
        BeltSlope slope = be.getBlockState()
                .getValue(BeltBlock.SLOPE);
        int verticality = slope == BeltSlope.DOWNWARD ? -1 : slope == BeltSlope.UPWARD ? 1 : 0;
        boolean slopeAlongX = beltFacing.getAxis() == Direction.Axis.X;
        boolean onContraption = be.getLevel() instanceof WrappedLevel;

        BeltInventory inventory = be.getInventory();
        for (TransportedItemStack transported : inventory.getTransportedItems())
            renderItem(be, partialTicks, ms, buffer, light, overlay, beltFacing, directionVec, slope, verticality,
                    slopeAlongX, onContraption, transported, beltStartOffset);
        if (inventory.getLazyClientItem() != null)
            renderItem(be, partialTicks, ms, buffer, light, overlay, beltFacing, directionVec, slope, verticality,
                    slopeAlongX, onContraption, inventory.getLazyClientItem(), beltStartOffset);

        ms.popPose();
    }

    private void renderItem(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
                            int overlay, Direction beltFacing, Vec3i directionVec, BeltSlope slope, int verticality, boolean slopeAlongX,
                            boolean onContraption, TransportedItemStack transported, Vec3 beltStartOffset) {

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
        float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);

        if (be.getSpeed() == 0) {
            offset = transported.beltPosition;
            sideOffset = transported.sideOffset;
        }

        Vec3 offsetVec = Vec3.ZERO;
        
        float startClimb = 2/16f;
        float endClimb = be.beltLength - 15/16f;

        if (offset < startClimb) {
            offsetVec = offsetVec.add(Vec3.atLowerCornerOf(beltFacing.getNormal()).scale(offset));
        } else if (offset < endClimb){
            offsetVec = offsetVec.add(Vec3.atLowerCornerOf(beltFacing.getNormal()).scale(1/16f));
            offsetVec = offsetVec.add(0,offset-startClimb,0);
        } else {
            offsetVec = offsetVec.add(0,be.beltLength-1,0);
            offsetVec = offsetVec.add(Vec3.atLowerCornerOf(beltFacing.getNormal()).scale(offset-endClimb));
        }

        Vec3 itemPos = beltStartOffset.add(
                        be.getBlockPos().getX(),
                        be.getBlockPos().getY(),
                        be.getBlockPos().getZ())
                .add(offsetVec);

        boolean onClimb = Mth.clamp(offset, startClimb, endClimb) == offset;
        float climbAngle = onClimb ? slopeAlongX ? 90 : -90 : 0;

        if (this.shouldCullItem(itemPos, be.getLevel())) {
            return;
        }

        ms.pushPose();
        TransformStack.of(ms).nudge(transported.angle);
        ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);

        boolean alongX = beltFacing.getClockWise()
                .getAxis() == Direction.Axis.X;
        if (!alongX)
            sideOffset *= -1;
        ms.translate(alongX ? sideOffset : 0, 0, alongX ? 0 : sideOffset);

        int stackLight;
        if (onContraption) {
            stackLight = light;
        } else {
            int segment = (int) Math.floor(offset);
            mutablePos.set(be.getBlockPos()).move(directionVec.getX() * segment, verticality * segment, directionVec.getZ() * segment);
            stackLight = LevelRenderer.getLightColor(be.getLevel(), mutablePos);
        }

        boolean renderUpright = BeltHelper.isItemUpright(transported.stack);
        BakedModel bakedModel = itemRenderer.getModel(transported.stack, be.getLevel(), null, 0);
        boolean blockItem = bakedModel.isGui3d();

        int count = 0;
        if (be.getLevel() instanceof PonderLevel || mc.player.getEyePosition(1.0F).distanceTo(itemPos) < 16)
            count = (int) (Mth.log2((int) (transported.stack.getCount()))) / 2;

        Random r = new Random(transported.angle);

        boolean slopeShadowOnly = renderUpright && onClimb;
        float slopeOffset = 1 / 8f;
        if (slopeShadowOnly)
            ms.pushPose();
        if (!renderUpright || slopeShadowOnly) {  // NOTE: Janky shadow rotation, replace later
            Direction dir = be.getBlockState().getValue(BeltBlock.HORIZONTAL_FACING).getClockWise();
            float angle = (dir == Direction.NORTH || dir == Direction.EAST) ? -climbAngle : climbAngle;
            ms.mulPose((slopeAlongX ? Axis.ZP : Axis.XP).rotationDegrees(angle));
        }
        ms.pushPose();
        ms.translate(0, -1 / 8f + 0.005f, 0);
        ShadowRenderHelper.renderShadow(ms, buffer, .75f, .2f);
        ms.popPose();
        if (slopeShadowOnly) {
            ms.popPose();
            ms.translate(0, slopeOffset, 0);
        }

        if (renderUpright) {
            Entity renderViewEntity = mc.cameraEntity;
            if (renderViewEntity != null) {
                Vec3 positionVec = renderViewEntity.position();
                Vec3 vectorForOffset = BeltHelper.getVectorForOffset(be, offset);
                Vec3 diff = vectorForOffset.subtract(positionVec);
                float yRot = (float) (Mth.atan2(diff.x, diff.z) + Math.PI);
                ms.mulPose(Axis.YP.rotation(yRot));
            }
            ms.translate(0, 3 / 32d, 1 / 16f);
        }

        for (int i = 0; i <= count; i++) {
            ms.pushPose();

            boolean box = PackageItem.isPackage(transported.stack);
            ms.mulPose(Axis.YP.rotationDegrees(transported.angle));
            if (!blockItem && !renderUpright) {
                ms.translate(0, -.09375, 0);
                ms.mulPose(Axis.XP.rotationDegrees(90));
            }

            if (blockItem && !box)
                ms.translate(r.nextFloat() * .0625f * i, 0, r.nextFloat() * .0625f * i);

            if (box) {
                ms.translate(0, 4 / 16f, 0);
                ms.scale(1.5f, 1.5f, 1.5f);
            } else {
                ms.scale(.5f, .5f, .5f);
            }

            itemRenderer.render(transported.stack, ItemDisplayContext.FIXED, false, ms, buffer, stackLight, overlay, bakedModel);
            ms.popPose();

            if (!renderUpright) {
                if (!blockItem)
                    ms.mulPose(Axis.YP.rotationDegrees(10));
                ms.translate(0, blockItem ? 1 / 64d : 1 / 16d, 0);
            } else
                ms.translate(0, 0, -1 / 16f);

        }

        ms.popPose();
    }


    public static PartialModel getVerticalBeltPartial(boolean start, boolean end, boolean bottom, boolean sticky) {
        if (sticky) {
            if (start || end) {
                return bottom ? STICKY_BELT_VERTICAL_START_OFFSET : STICKY_BELT_VERTICAL_START;
            } else {
                return bottom ? STICKY_BELT_MIDDLE_OFFSET : STICKY_BELT_MIDDLE;
            }
        } else {
            if (start || end) {
                return bottom ? BELT_VERTICAL_START_OFFSET : BELT_VERTICAL_START;
            } else {
                return bottom ? AllPartialModels.BELT_MIDDLE_BOTTOM : AllPartialModels.BELT_MIDDLE;
            }
        }
    }

    public static SpriteShiftEntry getVerticalSpriteShiftEntry(DyeColor color, boolean bottom, boolean sticky) {
        if (sticky) {
            if (color != null) {
                return (bottom ? STICKY_DYED_OFFSET_BELTS : STICKY_DYED_BELTS).get(color);
            } else {
                return bottom ? STICKY_BELT_OFFSET : STICKY_BELT;
            }
        } else {
            return getSpriteShiftEntry(color, false, bottom);
        }
    }

    public static void init() {}
}

package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.*;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.List;

// NOTE: BeltPart.START should always be at the bottom
@MethodsReturnNonnullByDefault
public class VerticalBeltBlock extends BeltBlock {

    public VerticalBeltBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<BeltBlockEntity> getBlockEntityClass() {
        // NOTE: may cause runtime errors, so far I think I'm safe
        return (Class<BeltBlockEntity>) (Class<?>) VerticalBeltBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BeltBlockEntity> getBlockEntityType() {
        return VerticalAdditions.VERTICAL_BELT_BLOCK_ENTITY.get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return VerticalBeltShapes.getShape(state);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
                                       Player player) {
        return VerticalAdditions.VERTICAL_BELT_CONNECTOR.asStack();
    }

    // Only change is the isConnector check
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.mayBuild())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean isWrench = AllItems.WRENCH.isIn(stack);
        boolean isConnector = VerticalAdditions.VERTICAL_BELT_CONNECTOR.isIn(stack);
        boolean isShaft = AllBlocks.SHAFT.isIn(stack);
        boolean isDye = stack.is(Tags.Items.DYES);
        boolean hasWater = GenericItemEmptying.emptyItem(level, stack, true)
                .getFirst()
                .getFluid()
                .isSame(Fluids.WATER);
        boolean isHand = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;

        if (isDye || hasWater)
            return onBlockEntityUseItemOn(level, pos,
                    be -> be.applyColor(DyeColor.getColor(stack)) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);

        if (isConnector)
            return BeltSlicer.useConnector(state, level, pos, player, hand, hitResult, new BeltSlicer.Feedback());
        if (isWrench)
            return BeltSlicer.useWrench(state, level, pos, player, hand, hitResult, new BeltSlicer.Feedback());

        BeltBlockEntity belt = BeltHelper.getSegmentBE(level, pos);
        if (belt == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (PackageItem.isPackage(stack)) {
            ItemStack toInsert = stack.copy();
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, belt.getBlockPos(), null);
            if (handler == null)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            if (remainder.isEmpty()) {
                stack.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (isHand) {
            BeltBlockEntity controllerBelt = belt.getControllerBE();
            if (controllerBelt == null)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            MutableBoolean success = new MutableBoolean(false);
            controllerBelt.getInventory()
                    .applyToEachWithin(belt.index + .5f, .55f, (transportedItemStack) -> {
                        player.getInventory()
                                .placeItemBackInInventory(transportedItemStack.stack);
                        success.setTrue();
                        return TransportedItemStackHandlerBehaviour.TransportedResult.removeItem();
                    });
            if (success.isTrue())
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f,
                        1f + level.random.nextFloat());
        }

        if (isShaft) {
            if (state.getValue(PART) != BeltPart.MIDDLE)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            if (!player.isCreative())
                stack.shrink(1);
            KineticBlockEntity.switchToBlockState(level, pos, state.setValue(PART, BeltPart.PULLEY));
            return ItemInteractionResult.SUCCESS;
        }

        if (AllBlocks.BRASS_CASING.isIn(stack)) {
            withBlockEntityDo(level, pos, be -> be.setCasingType(BeltBlockEntity.CasingType.BRASS));
            updateCoverProperty(level, pos, level.getBlockState(pos));

            SoundType soundType = AllBlocks.BRASS_CASING.getDefaultState()
                    .getSoundType(level, pos, player);
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            return ItemInteractionResult.SUCCESS;
        }

        if (AllBlocks.ANDESITE_CASING.isIn(stack)) {
            withBlockEntityDo(level, pos, be -> be.setCasingType(BeltBlockEntity.CasingType.ANDESITE));
            updateCoverProperty(level, pos, level.getBlockState(pos));

            SoundType soundType = AllBlocks.ANDESITE_CASING.getDefaultState()
                    .getSoundType(level, pos, player);
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        List<ItemStack> required = new ArrayList<>();
        if (state.getValue(PART) != BeltPart.MIDDLE)
            required.add(AllBlocks.SHAFT.asStack());
        if (state.getValue(PART) == BeltPart.START)
            required.add(VerticalAdditions.VERTICAL_BELT_CONNECTOR.asStack());
        if (required.isEmpty())
            return ItemRequirement.NONE;
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, required);
    }
}

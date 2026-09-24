package com.kreidev.cmverticaladditions;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

public class VerticalBeltBlockEntity extends BeltBlockEntity {

    public VerticalBeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VerticalAdditionsContent.VERTICAL_BELT_BLOCK_ENTITY.get(),
                (be, context) -> {
                    if (!BeltBlock.canTransportObjects(be.getBlockState()))
                        return null;
                    if (!be.isRemoved() && be.itemHandler == null) {
                        be.initializeItemHandler();
                    }

                    return be.itemHandler;
                }
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        if (behaviours.removeIf(b -> b.getType() == DirectBeltInputBehaviour.TYPE)) {
            behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::canInsertFrom)
                    .setInsertionHandler(this::tryInsertingFromSide).considerOccupiedWhen(this::isOccupied));
        }
    }

    private boolean canInsertFrom(Direction side) {
        if (getSpeed() == 0)
            return false;
        BlockState state = getBlockState();
        if (state.hasProperty(BeltBlock.SLOPE) && (state.getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS))
            return false;

        return getMovementFacing() != side.getOpposite();
    }

    public boolean isOccupied(Direction side) {
        BeltBlockEntity nextBeltController = getControllerBE();
        if (nextBeltController == null)
            return true;
        BeltInventory nextInventory = nextBeltController.getInventory();
        if (nextInventory == null)
            return true;
        if (getSpeed() == 0)
            return true;
        if (getMovementFacing() == side.getOpposite())
            return true;
        if (!nextInventory.canInsertAtFromSide(index, side))
            return true;
        return false;
    }

    public ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
        BeltBlockEntity nextBeltController = getControllerBE();
        ItemStack inserted = transportedStack.stack;
        ItemStack empty = ItemStack.EMPTY;

        if (!BeltBlock.canTransportObjects(getBlockState()))
            return inserted;
        if (nextBeltController == null)
            return inserted;
        BeltInventory nextInventory = nextBeltController.getInventory();
        if (nextInventory == null)
            return inserted;

        BlockEntity teAbove = level.getBlockEntity(worldPosition.above());
        if (teAbove instanceof BrassTunnelBlockEntity tunnelBE) {
            if (tunnelBE.hasDistributionBehaviour()) {
                if (!tunnelBE.getStackToDistribute()
                        .isEmpty())
                    return inserted;
                if (!tunnelBE.testFlapFilter(side.getOpposite(), inserted))
                    return inserted;
                if (!simulate) {
                    BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);
                    tunnelBE.setStackToDistribute(inserted, side.getOpposite());
                }
                return empty;
            }
        }

        if (isOccupied(side))
            return inserted;
        if (simulate)
            return empty;

        transportedStack = transportedStack.copy();
        transportedStack.beltPosition = index + .5f - Math.signum(getDirectionAwareBeltMovementSpeed()) / 16f;

        Direction movementFacing = getMovementFacing();
        if (!side.getAxis()
                .isVertical()) {
            if (movementFacing != side) {
                transportedStack.sideOffset = side.getAxisDirection()
                        .getStep() * .675f;
                if (side.getAxis() == Direction.Axis.X)
                    transportedStack.sideOffset *= -1;
            } else {
                // This creates a smoother transition from belt to belt
                float extraOffset = transportedStack.prevBeltPosition != 0
                        && BeltHelper.getSegmentBE(level, worldPosition.relative(movementFacing.getOpposite())) != null
                        ? .26f
                        : 0;
                transportedStack.beltPosition =
                        getDirectionAwareBeltMovementSpeed() > 0 ? index - extraOffset : index + 1 + extraOffset;
            }
        }

        transportedStack.prevSideOffset = transportedStack.sideOffset;
        transportedStack.insertedAt = index;
        transportedStack.insertedFrom = side;
        transportedStack.prevBeltPosition = transportedStack.beltPosition;

        BeltTunnelInteractionHandler.flapTunnel(nextInventory, index, side.getOpposite(), true);

        nextInventory.addItem(transportedStack);
        nextBeltController.setChanged();
        nextBeltController.sendData();
        return empty;
    }
}

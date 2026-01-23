package com.kreidev.cmverticaladditions;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class VerticalBeltBlockEntity extends BeltBlockEntity {

    public VerticalBeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VerticalAdditions.VERTICAL_BELT_BLOCK_ENTITY.get(),
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
    public void tick() {
        super.tick();
    }
}

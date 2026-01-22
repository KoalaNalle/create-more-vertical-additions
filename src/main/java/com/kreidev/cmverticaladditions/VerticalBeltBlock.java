package com.kreidev.cmverticaladditions;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
}

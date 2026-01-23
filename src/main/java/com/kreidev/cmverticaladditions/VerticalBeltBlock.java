package com.kreidev.cmverticaladditions;

import com.simibubi.create.content.kinetics.belt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// NOTE: BeltPart.START should always be at the bottom
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
}

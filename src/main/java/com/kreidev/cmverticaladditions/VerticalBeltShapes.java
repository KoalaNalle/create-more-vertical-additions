package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.world.level.block.Block.box;

// TODO: replace with own implementation
public class VerticalBeltShapes {

    private static final VoxelShaper
            VERTICAL_MIDDLE = VoxelShaper.forHorizontal(makeVerticalMiddle(), Direction.SOUTH),
            VERTICAL_START = VoxelShaper.forHorizontal(makeVerticalStart(), Direction.SOUTH),
            VERTICAL_END = VoxelShaper.forHorizontal(makeVerticalEnd(), Direction.SOUTH);

    private static final VoxelShaper
            PARTIAL_CASING_MIDDLE = VoxelShaper.forHorizontal(box(0, 0, 5, 16, 11, 16), Direction.SOUTH),
            PARTIAL_CASING_START = VoxelShaper.forHorizontal(box(0, 0, 5, 16, 11, 16), Direction.SOUTH),
            PARTIAL_CASING_END = VoxelShaper.forHorizontal(box(0, 0, 5, 16, 11, 16), Direction.SOUTH);

    private static final Map<BlockState, VoxelShape> cache = new HashMap<>();
    private static final Map<BlockState, VoxelShape> collisionCache = new HashMap<>();

    public static VoxelShape getShape(BlockState state) {
        if (cache.containsKey(state))
            return cache.get(state);
        VoxelShape createdShape = Shapes.or(getBeltShape(state), getCasingShape(state));
        cache.put(state, createdShape);
        return createdShape;
    }

    public static VoxelShape getCollisionShape(BlockState state) {
        if (collisionCache.containsKey(state))
            return collisionCache.get(state);
        VoxelShape createdShape = Shapes.joinUnoptimized(AllShapes.BELT_COLLISION_MASK, getShape(state), BooleanOp.AND);
        collisionCache.put(state, createdShape);
        return createdShape;
    }

    private static VoxelShape getBeltShape(BlockState state) {
        Direction facing = state.getValue(BeltBlock.HORIZONTAL_FACING);
        Direction.Axis axis = facing.getAxis();
        BeltPart part = state.getValue(BeltBlock.PART);
        BeltSlope slope = state.getValue(BeltBlock.SLOPE);

        if (slope == BeltSlope.VERTICAL) {
            return switch (part) {
                case MIDDLE, PULLEY -> VERTICAL_MIDDLE.get(axis);
                case START -> VERTICAL_START.get(axis);
                case END -> VERTICAL_END.get(axis);
            };
        }

        //bad state
        return Shapes.empty();
    }

    private static VoxelShape getCasingShape(BlockState state) {
        if (!state.getValue(BeltBlock.CASING))
            return Shapes.empty();

        Direction facing = state.getValue(BeltBlock.HORIZONTAL_FACING);
        BeltPart part = state.getValue(BeltBlock.PART);
        BeltSlope slope = state.getValue(BeltBlock.SLOPE);

        if (slope == BeltSlope.VERTICAL && part == BeltPart.MIDDLE)
            return Shapes.empty();

        //something went wrong
        return Shapes.block();
    }

    private static VoxelShape makeVerticalMiddle() {
        return box(1,0,3,15,16,13);
    }

    private static VoxelShape makeVerticalStart() {
        return Shapes.or(
                box(1,4,0,15,12,12),
                box(1,3,1,15,13,12),
                box(1,12,3,15,16,13)
        );
    }

    private static VoxelShape makeVerticalEnd() {
        return Shapes.or(
                box(1,4,4,15,12,16),
                box(1,3,4,15,13,15),
                box(1,0,3,15,4,13)
        );
    }
}

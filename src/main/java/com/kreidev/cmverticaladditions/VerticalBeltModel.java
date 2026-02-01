package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

import static com.kreidev.cmverticaladditions.VerticalAdditions.resLoc;

@MethodsReturnNonnullByDefault
public class VerticalBeltModel extends BeltModel {

    private static final SpriteShiftEntry SPRITE_SHIFT = SpriteShifter
            .get(resLoc("block/brass_vertical_belt_casing"), resLoc("block/andesite_vertical_belt_casing"));

    public VerticalBeltModel(BakedModel template) {
        super(template);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        if (!extraData.has(CASING_PROPERTY))
            return quads;

        boolean cover = extraData.get(COVER_PROPERTY);
        BeltBlockEntity.CasingType type = extraData.get(CASING_PROPERTY);
        boolean brassCasing = type == BeltBlockEntity.CasingType.BRASS;

        if (type == BeltBlockEntity.CasingType.NONE || brassCasing && !cover)
            return quads;

        quads = new ArrayList<>(quads);

        if (cover) {
            boolean alongX = state.getValue(BeltBlock.HORIZONTAL_FACING)
                    .getAxis() == Direction.Axis.X;
            BakedModel coverModel =
                    (brassCasing ? alongX ? AllPartialModels.BRASS_BELT_COVER_X : AllPartialModels.BRASS_BELT_COVER_Z
                            : alongX ? AllPartialModels.ANDESITE_BELT_COVER_X : AllPartialModels.ANDESITE_BELT_COVER_Z).get();
            quads.addAll(coverModel.getQuads(state, side, rand, extraData, renderType));
        }

        if (brassCasing)
            return quads;

        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite original = quad.getSprite();
            if (original != SPRITE_SHIFT.getOriginal())
                continue;

            BakedQuad newQuad = BakedQuadHelper.clone(quad);
            int[] vertexData = newQuad.getVertices();

            for (int vertex = 0; vertex < 4; vertex++) {
                float u = BakedQuadHelper.getU(vertexData, vertex);
                float v = BakedQuadHelper.getV(vertexData, vertex);
                BakedQuadHelper.setU(vertexData, vertex, SPRITE_SHIFT.getTargetU(u));
                BakedQuadHelper.setV(vertexData, vertex, SPRITE_SHIFT.getTargetV(v));
            }

            quads.set(i, newQuad);
        }

        return quads;
    }
}

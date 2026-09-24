package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltGenerator;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import static com.kreidev.cmverticaladditions.VerticalAdditions.REGISTRATE;
import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

/** Initialized by the mod constructor after Registrate has its own mod event bus. */
public final class VerticalAdditionsContent {
    static {
        if (REGISTRATE.getModEventBus() == null) {
            throw new IllegalStateException("Attach the Vertical Belts mod event bus before declaring content");
        }
    }

    @SuppressWarnings("removal")
    public static final BlockEntry<VerticalBeltBlock> VERTICAL_BELT_BLOCK = REGISTRATE.block("vertical_belt", VerticalBeltBlock::new)
            .properties(p -> p.sound(SoundType.WOOL)
                    .strength(0.8f)
                    .mapColor(MapColor.COLOR_GRAY))
            .addLayer(() -> RenderType::cutoutMipped)
            .transform(axeOrPickaxe())
            .blockstate(new BeltGenerator()::generate)
            .transform(displaySource(AllDisplaySources.ITEM_NAMES))
            .onRegister(CreateRegistrate.blockModel(() -> VerticalBeltModel::new))
            .clientExtension(() -> BeltBlock.RenderProperties::new)
            .register();

    public static final BlockEntityEntry<VerticalBeltBlockEntity> VERTICAL_BELT_BLOCK_ENTITY = REGISTRATE
            .blockEntity("vertical_belt", VerticalBeltBlockEntity::new)
//            .visual(() -> BeltVisual::new, BeltBlockEntity::shouldRenderNormally)
            .validBlocks(VERTICAL_BELT_BLOCK)
//            .renderer(() -> VerticalBeltRenderer::new)
            .register();

    public static final ItemEntry<VerticalBeltConnectorItem> VERTICAL_BELT_CONNECTOR = REGISTRATE
            .item("vertical_belt_connector", VerticalBeltConnectorItem::new)
//            .lang("Mechanical Belt")
            .register();

    private VerticalAdditionsContent() {}

    public static void register() {
        // Calling this method initializes the declarations above exactly once.
    }
}

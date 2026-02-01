package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.content.kinetics.belt.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;

import static com.simibubi.create.api.behaviour.display.DisplaySource.displaySource;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

@Mod(VerticalAdditions.MOD_ID)
public class VerticalAdditions {
    public static final String MOD_ID = "cmverticaladditions";

    @SuppressWarnings("unused")
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate
            .create(MOD_ID)
            .defaultCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey());

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


    public VerticalAdditions(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        REGISTRATE.registerEventListeners(modEventBus);
        modEventBus.addListener(VerticalAdditions::clientInit);
        modEventBus.addListener(ClientConfig::onLoad);
        modEventBus.addListener(ClientConfig::onReload);
        modEventBus.addListener(VerticalBeltBlockEntity::registerCapabilities);
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        // Something's wrong with registrate that makes me wanna commit seppuku
        BlockEntityRenderers.register(
                VERTICAL_BELT_BLOCK_ENTITY.get(),
                VerticalBeltRenderer::new
        );
        VerticalBeltRenderer.init();
    }


    public static ResourceLocation resLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

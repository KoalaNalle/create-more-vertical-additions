package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;

@Mod(VerticalAdditions.MOD_ID)
public class VerticalAdditions {
    public static final String MOD_ID = "cmverticaladditions";

    @SuppressWarnings("unused")
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public VerticalAdditions(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        REGISTRATE.registerEventListeners(modEventBus);
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey());
        // Builders attach client listeners immediately only when their owner's bus is already bound.
        VerticalAdditionsContent.register();
        modEventBus.addListener(VerticalAdditions::clientInit);
        modEventBus.addListener(ClientConfig::onLoad);
        modEventBus.addListener(ClientConfig::onReload);
        modEventBus.addListener(VerticalBeltBlockEntity::registerCapabilities);
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        // Something's wrong with registrate that makes me wanna commit seppuku
        BlockEntityRenderers.register(
                VerticalAdditionsContent.VERTICAL_BELT_BLOCK_ENTITY.get(),
                VerticalBeltRenderer::new
        );
        VerticalBeltRenderer.init();
    }


    public static ResourceLocation resLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

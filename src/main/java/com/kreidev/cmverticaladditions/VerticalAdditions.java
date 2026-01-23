package com.kreidev.cmverticaladditions;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.content.kinetics.belt.*;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
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
            .onRegister(CreateRegistrate.blockModel(() -> BeltModel::new))
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
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        REGISTRATE.registerEventListeners(modEventBus);
        modEventBus.addListener(VerticalAdditions::clientInit);
        modEventBus.addListener(CommonConfig::onLoad);
        modEventBus.addListener(CommonConfig::onReload);
        modEventBus.addListener(VerticalBeltBlockEntity::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(VerticalAdditions::serverTick);
    }

    public static void clientInit(final FMLClientSetupEvent event) {
        // Something's wrong with registrate that makes me wanna commit seppuku
        BlockEntityRenderers.register(
                VERTICAL_BELT_BLOCK_ENTITY.get(),
                VerticalBeltRenderer::new
        );
    }

    public static void serverTick(ServerTickEvent.Pre event) {
        ServerPlayer player = event.getServer().getPlayerList().getPlayerByName("Dev");
        if (player == null) return;

        Level level = player.level();
        if (level.getGameTime()%10!=0) return;

        if (player.pick(player.blockInteractionRange(), 0.0F, false) instanceof BlockHitResult hit
                && level.getBlockState(hit.getBlockPos()).getBlock() instanceof BeltBlock) {
            BlockPos pos = hit.getBlockPos();
            BeltBlockEntity controller = BeltHelper.getControllerBE(level, pos);

            VerticalAdditions.LOGGER.debug("pos: " + pos);

            if (!(BeltHelper.getSegmentBE(level, pos) instanceof BeltBlockEntity be)) return;

            VerticalAdditions.LOGGER.debug("segment: " + be);

            VerticalAdditions.LOGGER.debug("be length: " + be.beltLength);


            VerticalAdditions.LOGGER.debug("controller: " + controller);

//            VerticalAdditions.LOGGER.debug("segment: " + be.getController());

//            VerticalAdditions.LOGGER.debug("controller pos: " + controller.getBlockPos());

//            VerticalAdditions.LOGGER.debug("length: " + controller.beltLength);

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);

//            BeltInventory inventory = controller.getInventory();

//            if (inventory != null) {
//                VerticalAdditions.LOGGER.debug("has inventory");
//            }

            VerticalAdditions.LOGGER.debug("handler: " + handler);

            if (handler != null) {
                VerticalAdditions.LOGGER.debug("has handler");
            }


//            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
//            BeltBlockEntity controller = BeltHelper.getControllerBE(level, pos);
//            Outliner.getInstance().showAABB("test belt", shape.bounds());
        }
    }


    public static ResourceLocation resLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}

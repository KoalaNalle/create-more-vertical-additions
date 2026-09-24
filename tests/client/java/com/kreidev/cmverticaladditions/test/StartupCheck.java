package com.kreidev.cmverticaladditions.test;

import com.google.common.collect.Table;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kreidev.cmverticaladditions.VerticalAdditions;
import com.kreidev.cmverticaladditions.VerticalBeltModel;
import com.kreidev.cmverticaladditions.VerticalBeltRenderer;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.common.NeoForge;

/** Runs in a separate development mod, never included in the shipped artifact. No world is opened. */
@Mod("cmverticaladditions_startup_check")
public final class StartupCheck {
    private boolean finished;
    private final Field gameLoadFinished;

    public StartupCheck() throws ReflectiveOperationException {
        gameLoadFinished = Minecraft.class.getDeclaredField("gameLoadFinished");
        gameLoadFinished.setAccessible(true);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (finished || client.getOverlay() != null) return;
        try {
            // A pack may show its onboarding screen instead of TitleScreen after loading.
            if (!gameLoadFinished.getBoolean(client)) return;
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot determine client load completion", failure);
        }
        finished = true;
        JsonObject report = new JsonObject();
        report.addProperty("completed_at_utc", Instant.now().toString());
        report.addProperty("game_load_finished", true);
        report.addProperty("screen_class", client.screen == null ? "none" : client.screen.getClass().getName());
        report.addProperty("world_opened", client.level != null);
        try {
            // Registrate retains deferred entries even after flushing: zero proves our declarations
            // never entered the shared, load-order-sensitive queue in the first place.
            var field = OneTimeEventReceiver.class.getDeclaredField("waitingModListeners");
            field.setAccessible(true);
            Table<?, ?, ?> waiting = (Table<?, ?, ?>) field.get(null);
            long deferred = waiting.cellSet().stream()
                    .filter(cell -> cell.getRowKey() == VerticalAdditions.REGISTRATE)
                    .mapToLong(cell -> ((List<?>) cell.getValue()).size()).sum();
            var id = VerticalAdditions.resLoc("vertical_belt");
            var block = BuiltInRegistries.BLOCK.get(id);
            var extension = IClientBlockExtensions.of(block);
            var model = client.getBlockRenderer().getBlockModel(block.defaultBlockState());
            var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
            var entity = type.create(BlockPos.ZERO, block.defaultBlockState());
            net.minecraft.client.renderer.blockentity.BlockEntityRenderer<?> renderer =
                    client.getBlockEntityRenderDispatcher().getRenderer(entity);
            report.addProperty("deferred_registrate_callbacks", deferred);
            report.addProperty("extension_class", extension.getClass().getName());
            report.addProperty("model_class", model.getClass().getName());
            report.addProperty("renderer_class", renderer == null ? "missing" : renderer.getClass().getName());
            report.addProperty("passed", client.level == null && deferred == 0 && extension instanceof BeltBlock.RenderProperties
                    && model instanceof VerticalBeltModel && renderer instanceof VerticalBeltRenderer);
        } catch (Exception failure) {
            report.addProperty("passed", false);
            report.addProperty("error", failure.toString());
        }
        report.add("loaded_mods", new com.google.gson.Gson().toJsonTree(ModList.get().getMods().stream()
                .map(mod -> mod.getModId() + "@" + mod.getVersion()).sorted().toList()));
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("startup-check.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report), StandardOpenOption.CREATE_NEW);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not preserve startup-check evidence", failure);
        } finally {
            client.execute(client::stop);
        }
    }
}

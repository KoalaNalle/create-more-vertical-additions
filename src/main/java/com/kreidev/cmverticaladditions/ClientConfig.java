package com.kreidev.cmverticaladditions;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@SuppressWarnings("unused")
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue STICKY_TEXTURES = BUILDER
            .comment("Use the sticky belt textures on vertical belts")
            .define("stickyTextures", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean stickyTextures;

    private static void updateConfigs() {
        stickyTextures = STICKY_TEXTURES.get();
    }

    static void onLoad(final ModConfigEvent.Loading unused) {
        updateConfigs();
    }

    static void onReload(final ModConfigEvent.Reloading event) {
        updateConfigs();
    }
}

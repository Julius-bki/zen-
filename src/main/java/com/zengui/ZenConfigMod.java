package com.zengui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.class_310; // MinecraftClient
import net.minecraft.class_437; // Screen

import com.google.gson.*;
import com.mojang.brigadier.context.CommandContext;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ZenConfigMod implements ClientModInitializer {

    // Config path relative to MC run directory: config/zen/config/Config.json
    private static final String CONFIG_RELATIVE = "config/zen/config/Config.json";
    private static final String ZEN_CLICK_GUI_CLASS = "xyz.meowing.zen.config.ui.ClickGUI";

    private static final ScheduledExecutorService SCREEN_WATCHER = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "zen-gui-screen-bridge");
            t.setDaemon(true);
            return t;
        }
    });

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("zconf")
                    .executes(ctx -> {
                        openConfigScreen(ctx);
                        return 1;
                    })
            );
        });

        // Compatibility bridge: if original Zen opens its ClickGUI, replace it with this GUI.
        SCREEN_WATCHER.scheduleAtFixedRate(() -> {
            try {
                class_310 client = class_310.method_1551();
                if (client == null) return;
                client.execute(() -> tryReplaceZenClickGui(client));
            } catch (Throwable ignored) {
                // Keep watcher alive even if one tick fails.
            }
        }, 500L, 120L, TimeUnit.MILLISECONDS);
    }

    private static void openConfigScreen(CommandContext<FabricClientCommandSource> ctx) {
        class_310 client = ctx.getSource().getClient();
        final File finalFile = new File(client.field_1697, CONFIG_RELATIVE);
        final JsonObject finalRoot = loadConfigRoot(finalFile);

        // Schedule opening the screen on the main thread
        client.execute(() -> client.method_1507(new ZenConfigScreen(finalFile, finalRoot)));
    }

    private static JsonObject loadConfigRoot(File configFile) {
        if (!configFile.exists()) return new JsonObject();
        try (Reader r = new FileReader(configFile)) {
            JsonElement element = JsonParser.parseReader(r);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static void tryReplaceZenClickGui(class_310 client) {
        class_437 current = client.field_1755;
        if (current == null) return;
        if (!ZEN_CLICK_GUI_CLASS.equals(current.getClass().getName())) return;

        File configFile = new File(client.field_1697, CONFIG_RELATIVE);
        JsonObject root = loadConfigRoot(configFile);
        client.method_1507(new ZenConfigScreen(configFile, root));
    }
}

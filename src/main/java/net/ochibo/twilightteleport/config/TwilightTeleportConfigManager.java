package net.ochibo.twilightteleport.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.ochibo.twilightteleport.TwilightTeleport;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TwilightTeleportConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("twilightteleport.json");

    private static TwilightTeleportConfig config =
            TwilightTeleportConfig.defaults();

    private TwilightTeleportConfigManager() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            config = TwilightTeleportConfig.defaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            TwilightTeleportConfig loaded = GSON.fromJson(
                    reader,
                    TwilightTeleportConfig.class
            );

            config = loaded == null
                    ? TwilightTeleportConfig.defaults()
                    : loaded;
            config.sanitize();
        } catch (Exception exception) {
            TwilightTeleport.LOGGER.warn(
                    "Failed to load TwilightTeleport config. Using defaults.",
                    exception
            );
            config = TwilightTeleportConfig.defaults();
        }
    }

    public static void save() {
        config.sanitize();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            TwilightTeleport.LOGGER.warn(
                    "Failed to save TwilightTeleport config.",
                    exception
            );
        }
    }

    public static TwilightTeleportConfig get() {
        return config;
    }

    public static TwilightTeleportConfig copy() {
        return config.copy();
    }

    public static void set(TwilightTeleportConfig replacement) {
        config = replacement == null
                ? TwilightTeleportConfig.defaults()
                : replacement.copy();
        config.sanitize();
    }

    public static void reset() {
        config = TwilightTeleportConfig.defaults();
        save();
    }

}

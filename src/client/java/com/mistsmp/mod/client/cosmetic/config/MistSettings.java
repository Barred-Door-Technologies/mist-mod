package com.mistsmp.mod.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Client settings saved in config/mistsmp.json. Unknown keys are kept out; missing keys use defaults,
 * so a newer mod can add settings without breaking an older file.
 */
public record MistSettings(boolean showOtherPlayersParticles) {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MistSettings DEFAULTS = new MistSettings(true);

    private static volatile MistSettings current = DEFAULTS;

    public static MistSettings get() {
        return current;
    }

    public static void load() {
        Path file = file();
        if (!Files.exists(file)) {
            current = DEFAULTS;
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            boolean show = !json.has("showOtherPlayersParticles") || json.get("showOtherPlayersParticles").getAsBoolean();
            current = new MistSettings(show);
        } catch (Exception e) {
            LOGGER.warn("Could not read {}: {}. Using defaults.", file, e.getMessage());
            current = DEFAULTS;
        }
    }

    public static void update(MistSettings settings) {
        current = settings;
        Path file = file();
        JsonObject json = new JsonObject();
        json.addProperty("showOtherPlayersParticles", settings.showOtherPlayersParticles());
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.getMessage());
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("mistsmp.json");
    }
}
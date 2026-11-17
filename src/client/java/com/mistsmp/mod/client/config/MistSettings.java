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
 *
 * showOtherPlayersParticles: other players' cosmetic particles (K).
 * reduceTitleMotion: animated titles show as a still gradient (key unbound by default).
 */
public record MistSettings(boolean showOtherPlayersParticles, boolean reduceTitleMotion) {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MistSettings DEFAULTS = new MistSettings(true, false);

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
            boolean reduceMotion = json.has("reduceTitleMotion") && json.get("reduceTitleMotion").getAsBoolean();
            current = new MistSettings(show, reduceMotion);
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
        json.addProperty("reduceTitleMotion", settings.reduceTitleMotion());
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
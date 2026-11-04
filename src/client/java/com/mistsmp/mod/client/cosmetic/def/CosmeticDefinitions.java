package com.mistsmp.mod.client.cosmetic.def;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Loads every cosmetic definition file from the resource packs on each resource reload
 * (joining the server, accepting its pack, or F3+T while testing).
 *
 * File: assets/<ns>/cosmetics/<slot>/<key>.json  ->  cosmetic id <ns>:cosmetic/<slot>/<key>
 *
 * A broken file is logged and ignored; that cosmetic falls back to a still model instead of
 * failing the whole reload. Unknown fields are ignored, so the pack can add fields an older mod
 * doesn't understand yet.
 */
public final class CosmeticDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final String FOLDER = "cosmetics";

    private static volatile Map<Identifier, CosmeticDefinition> definitions = Map.of();

    private CosmeticDefinitions() {
    }

    public static void init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("mistsmp", "cosmetic_definitions"),
                (ResourceManagerReloadListener) CosmeticDefinitions::reload);
    }

    /** @return this cosmetic's definition, or a plain still-model definition if it has no file. */
    public static CosmeticDefinition get(Identifier cosmeticId) {
        CosmeticDefinition definition = definitions.get(cosmeticId);
        return definition != null ? definition : CosmeticDefinition.defaultFor(cosmeticId);
    }

    private static void reload(ResourceManager manager) {
        Map<Identifier, CosmeticDefinition> loaded = new ConcurrentHashMap<>();
        Map<Identifier, Resource> files = manager.listResources(FOLDER, path -> path.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            Identifier cosmeticId = toCosmeticId(file.getKey());
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(cosmeticId, parse(cosmeticId, json));
            } catch (Exception e) {
                LOGGER.warn("Bad cosmetic definition {}: {}. Using a still model instead.", file.getKey(), e.getMessage());
            }
        }

        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} cosmetic definitions", loaded.size());
    }

    /** mistsmp:cosmetics/head/top_hat.json -> mistsmp:cosmetic/head/top_hat */
    private static Identifier toCosmeticId(Identifier file) {
        String path = file.getPath();
        String inner = path.substring(FOLDER.length() + 1, path.length() - ".json".length());
        return Identifier.fromNamespaceAndPath(file.getNamespace(), "cosmetic/" + inner);
    }

    static CosmeticDefinition parse(Identifier id, JsonObject json) {
        List<Identifier> frames = new ArrayList<>();
        if (json.has("frames") && json.get("frames").isJsonArray()) {
            for (JsonElement frame : json.getAsJsonArray("frames")) {
                Identifier frameId = Identifier.tryParse(frame.getAsString());
                if (frameId != null) frames.add(frameId);
            }
        }
        if (frames.isEmpty()) frames.add(id);

        int frameTicks = Math.max(1, intOr(json, "frameTicks", 4));
        CosmeticDefinition.AnimateWhen animateWhen = CosmeticDefinition.AnimateWhen.parse(stringOr(json, "animateWhen", null));

        CosmeticDefinition.Motion motion = CosmeticDefinition.Motion.NONE;
        if (json.has("motion") && json.get("motion").isJsonObject()) {
            JsonObject m = json.getAsJsonObject("motion");
            float spin = floatOr(m, "spinDegreesPerSecond", 0f);
            float bobHeight = 0f;
            int bobPeriod = 40;
            if (m.has("bob") && m.get("bob").isJsonObject()) {
                JsonObject bob = m.getAsJsonObject("bob");
                bobHeight = floatOr(bob, "heightPixels", 0f);
                bobPeriod = Math.max(2, intOr(bob, "periodTicks", 40));
            }
            motion = new CosmeticDefinition.Motion(spin, bobHeight, bobPeriod);
        }

        CosmeticDefinition.Placement placement = CosmeticDefinition.Placement.NONE;
        if (json.has("placement") && json.get("placement").isJsonObject()) {
            JsonObject p = json.getAsJsonObject("placement");
            float[] offset = vec3(p, "offsetPixels");
            float[] rotation = vec3(p, "rotationDegrees");
            placement = new CosmeticDefinition.Placement(offset[0], offset[1], offset[2],
                    rotation[0], rotation[1], rotation[2], floatOr(p, "scale", 1f));
        }

        return new CosmeticDefinition(id, List.copyOf(frames), frameTicks, animateWhen, motion, placement, json);
    }

    private static float[] vec3(JsonObject obj, String key) {
        float[] out = {0f, 0f, 0f};
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray(key);
            for (int i = 0; i < Math.min(3, arr.size()); i++) {
                out[i] = arr.get(i).getAsFloat();
            }
        }
        return out;
    }

    private static int intOr(JsonObject obj, String key, int fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : fallback;
    }

    private static float floatOr(JsonObject obj, String key, float fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : fallback;
    }

    private static String stringOr(JsonObject obj, String key, String fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : fallback;
    }
}
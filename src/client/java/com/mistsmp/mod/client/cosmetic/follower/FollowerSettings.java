package com.mistsmp.mod.client.cosmetic.follower;

import com.google.gson.JsonObject;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;

/**
 * The "follower" section of a FOLLOWER cosmetic's definition file, plus the top-level "scale".
 * Distances are in blocks. Every field has a default, so a follower with no definition file still works.
 *
 * {
 *   "scale": 0.6,
 *   "follower": {
 *     "distance": 1.0,        blocks behind the player
 *     "height": 1.7,          blocks above the player's feet
 *     "side": 0.6,            blocks to the player's right (negative = left)
 *     "smoothing": 0.15,      share of the remaining gap closed each tick (0.01 to 1; lower = floatier)
 *     "orbitWhenIdle": { "afterTicks": 60, "radius": 0.9, "degreesPerSecond": 40 }
 *   }
 * }
 */
public record FollowerSettings(
        float scale,
        float distance,
        float height,
        float side,
        float smoothing,
        int orbitAfterTicks,
        float orbitRadius,
        float orbitDegreesPerSecond
) {

    /** Orbit is off when afterTicks is 0 or the section is missing. */
    public boolean orbits() {
        return orbitAfterTicks > 0 && orbitRadius > 0f && orbitDegreesPerSecond != 0f;
    }

    public static FollowerSettings from(CosmeticDefinition definition) {
        JsonObject raw = definition.raw();
        float scale = floatOr(raw, "scale", 1f);
        JsonObject f = raw.has("follower") && raw.get("follower").isJsonObject() ? raw.getAsJsonObject("follower") : new JsonObject();
        JsonObject orbit = f.has("orbitWhenIdle") && f.get("orbitWhenIdle").isJsonObject() ? f.getAsJsonObject("orbitWhenIdle") : null;

        return new FollowerSettings(
                clamp(scale, 0.05f, 4f),
                clamp(floatOr(f, "distance", 0.9f), -4f, 4f),
                clamp(floatOr(f, "height", 1.6f), -1f, 4f),
                clamp(floatOr(f, "side", 0.6f), -4f, 4f),
                clamp(floatOr(f, "smoothing", 0.15f), 0.01f, 1f),
                orbit == null ? 0 : Math.max(0, intOr(orbit, "afterTicks", 60)),
                orbit == null ? 0f : clamp(floatOr(orbit, "radius", 0.9f), 0f, 4f),
                orbit == null ? 0f : clamp(floatOr(orbit, "degreesPerSecond", 40f), -720f, 720f));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float floatOr(JsonObject obj, String key, float fallback) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int intOr(JsonObject obj, String key, int fallback) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
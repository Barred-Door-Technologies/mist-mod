package com.mistsmp.mod.client.cosmetic.follower;

import java.util.Locale;

import com.google.gson.JsonObject;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;

/**
 * The "follower" section of a FOLLOWER cosmetic's definition file, plus the top-level "scale".
 * Distances are in blocks. Every field has a default, so a follower with no definition file still works.
 *
 * Flying (the default): floats behind the player's shoulder and can circle them when idle.
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
 *
 * Walking: follows on the ground like a tamed wolf or cat. Models stand on y = 0 (feet on the bottom of
 * the grid), centered on x and z = 8, facing +Z.
 * {
 *   "follower": {
 *     "movement": "walking",
 *     "startDistance": 4,     starts walking after the player once they are further than this
 *     "stopDistance": 2,      stops once it is this close
 *     "speed": 0.2,           blocks per tick at a walk; speeds up as the gap grows (up to 3x)
 *     "teleportDistance": 12, further than this and it appears behind the player
 *     "wanderWhenIdle": { "afterTicks": 100, "radius": 2.5 }
 *   }
 * }
 * distance and side also place a walking follower when it teleports.
 */
public record FollowerSettings(
        float scale,
        Movement movement,
        float distance,
        float height,
        float side,
        float smoothing,
        int orbitAfterTicks,
        float orbitRadius,
        float orbitDegreesPerSecond,
        float startDistance,
        float stopDistance,
        float speed,
        float teleportDistance,
        int wanderAfterTicks,
        float wanderRadius
) {

    public enum Movement {
        FLYING, WALKING;

        static Movement parse(String value) {
            if (value == null) return FLYING;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return FLYING;
            }
        }
    }

    public boolean walks() {
        return movement == Movement.WALKING;
    }

    /** Orbit is off when afterTicks is 0 or the section is missing. Flying followers only. */
    public boolean orbits() {
        return !walks() && orbitAfterTicks > 0 && orbitRadius > 0f && orbitDegreesPerSecond != 0f;
    }

    /** Wandering is off when afterTicks is 0 or the section is missing. Walking followers only. */
    public boolean wanders() {
        return walks() && wanderAfterTicks > 0 && wanderRadius > 0f;
    }

    public static FollowerSettings from(CosmeticDefinition definition) {
        JsonObject raw = definition.raw();
        float scale = floatOr(raw, "scale", 1f);
        JsonObject f = raw.has("follower") && raw.get("follower").isJsonObject() ? raw.getAsJsonObject("follower") : new JsonObject();
        JsonObject orbit = f.has("orbitWhenIdle") && f.get("orbitWhenIdle").isJsonObject() ? f.getAsJsonObject("orbitWhenIdle") : null;
        JsonObject wander = f.has("wanderWhenIdle") && f.get("wanderWhenIdle").isJsonObject() ? f.getAsJsonObject("wanderWhenIdle") : null;
        Movement movement = Movement.parse(f.has("movement") && f.get("movement").isJsonPrimitive()
                ? f.get("movement").getAsString() : null);
        float stop = clamp(floatOr(f, "stopDistance", 2f), 0.5f, 16f);

        return new FollowerSettings(
                clamp(scale, 0.05f, 4f),
                movement,
                clamp(floatOr(f, "distance", 0.9f), -4f, 4f),
                clamp(floatOr(f, "height", 1.6f), -1f, 4f),
                clamp(floatOr(f, "side", 0.6f), -4f, 4f),
                clamp(floatOr(f, "smoothing", 0.15f), 0.01f, 1f),
                orbit == null ? 0 : Math.max(0, intOr(orbit, "afterTicks", 60)),
                orbit == null ? 0f : clamp(floatOr(orbit, "radius", 0.9f), 0f, 4f),
                orbit == null ? 0f : clamp(floatOr(orbit, "degreesPerSecond", 40f), -720f, 720f),
                // start is kept above stop so it can't flicker between walking and stopping
                Math.max(stop + 0.5f, clamp(floatOr(f, "startDistance", 4f), 1f, 24f)),
                stop,
                clamp(floatOr(f, "speed", 0.2f), 0.02f, 1f),
                clamp(floatOr(f, "teleportDistance", 12f), 4f, 48f),
                wander == null ? 0 : Math.max(0, intOr(wander, "afterTicks", 100)),
                wander == null ? 0f : clamp(floatOr(wander, "radius", 2.5f), 0f, 8f));
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
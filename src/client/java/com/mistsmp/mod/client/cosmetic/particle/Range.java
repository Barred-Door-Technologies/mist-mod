package com.mistsmp.mod.client.cosmetic.particle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraft.util.RandomSource;

/**
 * A number in a definition file that can be written as one value (3) or a range ([2, 5]).
 * A range is picked per particle, so particles from the same entry vary a little.
 */
public record Range(float min, float max) {

    public static Range of(float value) {
        return new Range(value, value);
    }

    public float pick(RandomSource random) {
        return min == max ? min : min + random.nextFloat() * (max - min);
    }

    /** Reads a number or a [min, max] pair, clamped to [low, high]. Anything unreadable gives the fallback. */
    public static Range parse(JsonElement element, Range fallback, float low, float high) {
        if (element == null || element.isJsonNull()) return fallback;
        try {
            if (element.isJsonPrimitive()) {
                float value = clamp(element.getAsFloat(), low, high);
                return new Range(value, value);
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.isEmpty()) return fallback;
                float a = clamp(array.get(0).getAsFloat(), low, high);
                float b = array.size() > 1 ? clamp(array.get(1).getAsFloat(), low, high) : a;
                return new Range(Math.min(a, b), Math.max(a, b));
            }
        } catch (RuntimeException e) {
            return fallback;
        }
        return fallback;
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

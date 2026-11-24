package com.mistsmp.mod.client.cosmetic.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

/**
 * The look and motion of a custom particle: an entry with "texture" or "textures" instead of "type".
 * Textures come from the resource pack's textures/particle folder, so new particle art ships with the
 * pack and never needs a mod release. Every field except the texture is optional; see docs/ASSETS.md.
 *
 * Ranges can be one number or [min, max] (see {@link Range}). Out-of-range values are clamped.
 */
public record CustomParticleSpec(
        List<Identifier> textures,
        FrameMode frameMode,
        int frameTicks,
        Range size,
        float sizeEnd,
        Integer colorA,
        Integer colorB,
        boolean useTint,
        Integer colorEnd,
        Range alpha,
        float fadeIn,
        float fadeOut,
        boolean glow,
        boolean cutout,
        Range rotation,
        Range spin,
        Range lifetime,
        float velocityRandomX,
        float velocityRandomY,
        float velocityRandomZ,
        float gravity,
        float drag,
        boolean collide,
        boolean follow,
        boolean orbit,
        float orbitRadius,
        float orbitSpeed,
        float orbitRise,
        float swayAmount,
        float swaySpeed,
        float pulseAmount,
        float pulseSpeed,
        int maxAlive
) {

    public enum FrameMode {
        RANDOM, LIFE, LOOP;

        static FrameMode parse(String value) {
            if (value == null) return RANDOM;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return RANDOM;
            }
        }
    }

    /** @return the spec, or null when the entry names no usable texture. */
    static CustomParticleSpec parse(JsonObject p) {
        List<Identifier> textures = new ArrayList<>();
        if (p.has("textures") && p.get("textures").isJsonArray()) {
            for (JsonElement element : p.getAsJsonArray("textures")) {
                Identifier id = element.isJsonPrimitive() ? textureId(element.getAsString()) : null;
                if (id != null) textures.add(id);
            }
        } else if (p.has("texture") && p.get("texture").isJsonPrimitive()) {
            Identifier id = textureId(p.get("texture").getAsString());
            if (id != null) textures.add(id);
        }
        if (textures.isEmpty()) return null;
        if (textures.size() > 32) textures = textures.subList(0, 32);

        // color: "#hex", "$tint", or ["#hex", "#hex"] (a random mix of the two per particle).
        Integer colorA = null;
        Integer colorB = null;
        boolean useTint = false;
        JsonElement color = p.get("color");
        if (color != null && color.isJsonPrimitive()) {
            String text = color.getAsString();
            useTint = "$tint".equalsIgnoreCase(text.trim());
            if (!useTint) colorA = ParticleSpec.parseColor(text);
        } else if (color != null && color.isJsonArray()) {
            JsonArray colors = color.getAsJsonArray();
            if (!colors.isEmpty()) colorA = ParticleSpec.parseColor(colors.get(0).getAsString());
            if (colors.size() > 1) colorB = ParticleSpec.parseColor(colors.get(1).getAsString());
        }
        String colorEndText = stringOr(p, "colorEnd", null);
        Integer colorEnd = ParticleSpec.parseColor(colorEndText);

        float[] velocityRandom = {0, 0, 0};
        if (p.has("velocityRandom") && p.get("velocityRandom").isJsonArray()) {
            JsonArray v = p.getAsJsonArray("velocityRandom");
            for (int i = 0; i < Math.min(3, v.size()); i++) {
                velocityRandom[i] = Range.clamp(v.get(i).getAsFloat(), 0f, 1f);
            }
        }

        JsonObject orbit = objectOrNull(p, "orbit");
        JsonObject sway = objectOrNull(p, "sway");
        JsonObject pulse = objectOrNull(p, "pulse");

        return new CustomParticleSpec(
                List.copyOf(textures),
                FrameMode.parse(stringOr(p, "frames", null)),
                (int) Range.clamp(floatOr(p, "frameTicks", 2f), 1f, 100f),
                Range.parse(p.get("size"), Range.of(0.1f), 0.01f, 1f),
                Range.clamp(floatOr(p, "sizeEnd", 1f), 0f, 4f),
                colorA,
                colorB,
                useTint,
                colorEnd,
                Range.parse(p.get("alpha"), Range.of(1f), 0f, 1f),
                Range.clamp(floatOr(p, "fadeIn", 0f), 0f, 1f),
                Range.clamp(floatOr(p, "fadeOut", 0.3f), 0f, 1f),
                boolOr(p, "glow", false),
                "cutout".equalsIgnoreCase(stringOr(p, "blend", "translucent")),
                p.has("rotation") ? Range.parse(p.get("rotation"), null, -360f, 360f) : null,
                Range.parse(p.get("spin"), Range.of(0f), -45f, 45f),
                Range.parse(p.get("lifetime"), new Range(20f, 30f), 1f, 200f),
                velocityRandom[0], velocityRandom[1], velocityRandom[2],
                Range.clamp(floatOr(p, "gravity", 0f), -0.2f, 0.2f),
                Range.clamp(floatOr(p, "drag", 0.96f), 0f, 1f),
                boolOr(p, "collide", false),
                boolOr(p, "follow", false),
                orbit != null,
                orbit == null ? 0f : Range.clamp(floatOr(orbit, "radius", 0.6f), 0f, 3f),
                orbit == null ? 0f : Range.clamp(floatOr(orbit, "speed", 4f), -45f, 45f),
                orbit == null ? 0f : Range.clamp(floatOr(orbit, "rise", 0f), -0.2f, 0.2f),
                sway == null ? 0f : Range.clamp(floatOr(sway, "amount", 0.1f), 0f, 1f),
                sway == null ? 0f : Range.clamp(floatOr(sway, "speed", 1f), 0f, 10f),
                pulse == null ? 0f : Range.clamp(floatOr(pulse, "amount", 0.2f), 0f, 1f),
                pulse == null ? 0f : Range.clamp(floatOr(pulse, "speed", 1f), 0f, 10f),
                (int) Range.clamp(floatOr(p, "maxAlive", 40f), 1f, 100f));
    }

    /**
     * Turns what an artist writes into a particle atlas name. The atlas names a file at
     * assets/mistsmp/textures/particle/cosmetic/heart.png as mistsmp:cosmetic/heart, but these are accepted too:
     * cosmetic/heart (namespace defaults to mistsmp), mistsmp:particle/cosmetic/heart, and a trailing .png.
     */
    static Identifier textureId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.trim();
        if (!text.contains(":")) text = "mistsmp:" + text;
        int colon = text.indexOf(':');
        String namespace = text.substring(0, colon);
        String path = text.substring(colon + 1);
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        if (path.startsWith("particle/")) path = path.substring("particle/".length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        return Identifier.tryParse(namespace + ":" + path);
    }

    private static JsonObject objectOrNull(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonObject() ? obj.getAsJsonObject(key) : null;
    }

    private static float floatOr(JsonObject obj, String key, float fallback) {
        try {
            return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean boolOr(JsonObject obj, String key, boolean fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsBoolean() : fallback;
    }

    private static String stringOr(JsonObject obj, String key, String fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : fallback;
    }
}

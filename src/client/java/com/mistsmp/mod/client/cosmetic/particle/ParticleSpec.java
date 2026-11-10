package com.mistsmp.mod.client.cosmetic.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * One entry of a definition's "particles" list.
 *
 * {
 *   "type": "minecraft:dust",   any vanilla particle that needs no extra data, plus dust and entity_effect (colored)
 *   "color": "#7A3CFF",         hex, or "$tint" to use the cosmetic's tint parameter (supporter title color)
 *   "size": 0.7,                dust only, 0.01 to 4
 *   "perSecond": 5,             spawn rate (fractions work: 0.5 = one every 2 seconds)
 *   "spread": 0.18,             blocks; how far from the anchor a particle can appear
 *   "area": "body",             AURA only: body (around the whole player), feet (ring at the feet), head (halo above)
 *   "velocity": [0, 0.02, 0],   blocks per tick; vanilla adds its own drift to some particles
 *   "when": "always"            always, moving, sprinting, flying, idle
 * }
 *
 * Followers spawn particles around the follower; auras around the player.
 */
public record ParticleSpec(
        ParticleType<?> type,
        Integer color,
        boolean useTint,
        float size,
        float perSecond,
        float spread,
        Area area,
        double velocityX,
        double velocityY,
        double velocityZ,
        CosmeticDefinition.AnimateWhen when
) {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    public enum Area {
        BODY, FEET, HEAD;

        static Area parse(String value) {
            if (value == null) return BODY;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return BODY;
            }
        }
    }

    /** @return the particle to spawn, using tint ("#RRGGBB" param) when the color is "$tint". */
    public ParticleOptions options(String tint) {
        int rgb = color != null ? color : DEFAULT_COLOR;
        if (useTint) {
            Integer parsed = parseColor(tint);
            if (parsed != null) rgb = parsed;
        }
        if (type == ParticleTypes.DUST) {
            return new DustParticleOptions(rgb, size);
        }
        if (type == ParticleTypes.ENTITY_EFFECT) {
            return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | rgb);
        }
        return (SimpleParticleType) type;
    }

    public static List<ParticleSpec> listFrom(CosmeticDefinition definition) {
        JsonObject raw = definition.raw();
        if (!raw.has("particles") || !raw.get("particles").isJsonArray()) {
            return List.of();
        }
        List<ParticleSpec> specs = new ArrayList<>();
        JsonArray list = raw.getAsJsonArray("particles");
        for (JsonElement element : list) {
            if (!element.isJsonObject()) continue;
            try {
                ParticleSpec spec = parse(element.getAsJsonObject());
                if (spec != null) specs.add(spec);
            } catch (Exception e) {
                LOGGER.warn("Bad particle entry in {}: {}", definition.id(), e.getMessage());
            }
        }
        return List.copyOf(specs);
    }

    private static ParticleSpec parse(JsonObject p) {
        String typeName = stringOr(p, "type", "minecraft:dust");
        Identifier typeId = Identifier.tryParse(typeName);
        ParticleType<?> type = typeId == null ? null : BuiltInRegistries.PARTICLE_TYPE.getValue(typeId);
        boolean supported = type == ParticleTypes.DUST || type == ParticleTypes.ENTITY_EFFECT || type instanceof SimpleParticleType;
        if (type == null || !supported) {
            LOGGER.warn("Particle type {} is not supported by this mod version; skipping it", typeName);
            return null;
        }

        String colorText = stringOr(p, "color", null);
        boolean useTint = "$tint".equalsIgnoreCase(colorText);
        Integer color = useTint ? null : parseColor(colorText);

        double[] velocity = {0, 0, 0};
        if (p.has("velocity") && p.get("velocity").isJsonArray()) {
            JsonArray v = p.getAsJsonArray("velocity");
            for (int i = 0; i < Math.min(3, v.size()); i++) {
                velocity[i] = clamp(v.get(i).getAsFloat(), -1f, 1f);
            }
        }

        return new ParticleSpec(
                type,
                color,
                useTint,
                clamp(floatOr(p, "size", 1f), 0.01f, 4f),
                clamp(floatOr(p, "perSecond", 4f), 0f, 40f),
                clamp(floatOr(p, "spread", 0.3f), 0f, 3f),
                Area.parse(stringOr(p, "area", null)),
                velocity[0], velocity[1], velocity[2],
                CosmeticDefinition.AnimateWhen.parse(stringOr(p, "when", null)));
    }

    static Integer parseColor(String text) {
        if (text == null) return null;
        String hex = text.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return null;
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static String stringOr(JsonObject obj, String key, String fallback) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : fallback;
    }
}
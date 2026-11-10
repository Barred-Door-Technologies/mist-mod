package com.mistsmp.mod.client.cosmetic.def;

import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

/**
 * How a cosmetic behaves, read from assets/mistsmp/cosmetics/<slot>/<key>.json in the resource pack.
 * Every field has a default, so a cosmetic with no definition file is a single still model.
 *
 * Units: placement offsets and bob height are in pixels (1/16 of a block, like Blockbench);
 * follower distances are in blocks.
 *
 * raw keeps the whole file so slot-specific sections (follower, particles, feet, held) can be read
 * by the code for that slot without changing this record.
 */
public record CosmeticDefinition(
        Identifier id,
        List<Identifier> frames,
        int frameTicks,
        AnimateWhen animateWhen,
        Motion motion,
        Placement placement,
        JsonObject raw
) {

    public enum AnimateWhen {
        ALWAYS, MOVING, SPRINTING, FLYING, IDLE;

        public static AnimateWhen parse(String value) {
            if (value == null) return ALWAYS;
            try {
                return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ALWAYS;
            }
        }
    }

    /** Continuous motion on top of the model's own placement. */
    public record Motion(float spinDegreesPerSecond, float bobHeightPixels, int bobPeriodTicks) {
        public static final Motion NONE = new Motion(0f, 0f, 40);

        public boolean isNone() {
            return spinDegreesPerSecond == 0f && bobHeightPixels == 0f;
        }
    }

    /** Extra offset (pixels), rotation (degrees, X/Y/Z) and uniform scale applied to the attached model. */
    public record Placement(float offsetX, float offsetY, float offsetZ,
                            float rotationX, float rotationY, float rotationZ, float scale) {
        public static final Placement NONE = new Placement(0f, 0f, 0f, 0f, 0f, 0f, 1f);

        public boolean isNone() {
            return this.equals(NONE);
        }
    }

    /** A cosmetic with no definition file: its own model, never animated, no extra transforms. */
    public static CosmeticDefinition defaultFor(Identifier id) {
        return new CosmeticDefinition(id, List.of(id), 4, AnimateWhen.ALWAYS, Motion.NONE, Placement.NONE, new JsonObject());
    }

    public boolean isAnimated() {
        return frames.size() > 1;
    }
}
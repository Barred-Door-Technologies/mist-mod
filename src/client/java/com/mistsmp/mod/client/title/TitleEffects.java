package com.mistsmp.mod.client.title;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mistsmp.mod.client.config.MistSettings;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Animated titles. The server sends a title as one text component per letter, already colored as a
 * still gradient, with a marker in each letter's insertion field:
 *
 *   mistfx:1;fx=shimmer;mo=none;gl=1;sp=4.0;a=4BECD7;b=FFFFFF;i=3;n=10
 *
 *   fx  color effect: static, flow, shimmer, pulse, rainbow, sparkle, ember
 *   mo  motion: none, wave, float, shake, glitch
 *   gl  1 for a soft breathing glow (colored text shadow)
 *   sp  seconds per cycle
 *   a,b the two colors (RRGGBB)
 *   i,n this letter's position and the title's length, brackets included
 *
 * The font renderer asks this class for each letter as it is drawn (FontGlyphMixin), so the title
 * animates wherever it appears: chat, chat history, and the tab list. Unknown values fall back to
 * the still gradient, so the server can add looks without breaking older mods. Everything here is
 * presentation only.
 *
 * Keep the math in step with the admin panel's preview (mist-ui TitlePreview.jsx).
 */
public final class TitleEffects {

    public static final String PREFIX = "mistfx:";

    /** The letter's new style and how far to move it, in GUI pixels. */
    public record Glyph(Style style, float dx, float dy) {
    }

    private record Fx(String effect, String motion, boolean glow, float period, int a, int b, int index, int count) {
        float pos() {
            return count <= 1 ? 0f : (float) index / (count - 1);
        }
    }

    private static final Fx INVALID = new Fx("", "", false, 1f, 0, 0, 0, 1);
    private static final Map<String, Fx> CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_LIMIT = 2048;

    private TitleEffects() {
    }

    public static boolean isMarker(String insertion) {
        return insertion != null && insertion.startsWith(PREFIX);
    }

    /**
     * The animated look for this letter right now, or null if it isn't part of an animated title
     * (the letter is then drawn exactly as vanilla would).
     */
    public static Glyph apply(Style style) {
        if (style == null || !isMarker(style.getInsertion())) return null;
        Fx fx = parse(style.getInsertion());
        if (fx == INVALID) return null;

        float pos = fx.pos();
        if (MistSettings.get().reduceTitleMotion()) {
            return new Glyph(style.withColor(TextColor.fromRgb(lerp(fx.a, fx.b, pos))), 0f, 0f);
        }

        double seconds = System.currentTimeMillis() / 1000.0;
        float phase = (float) ((seconds / fx.period) % 1.0);
        long cycle = (long) Math.floor(seconds / fx.period);

        Style out = style.withColor(TextColor.fromRgb(color(fx, pos, phase, cycle, seconds)));
        if (fx.glow) {
            out = out.withShadowColor(glow(fx.a, phase));
        }

        float dx = 0f;
        float dy = 0f;
        switch (fx.motion) {
            case "wave" -> dy = (float) Math.sin(2 * Math.PI * (phase - pos * 0.5f));
            case "float" -> dy = (float) Math.sin(2 * Math.PI * phase);
            case "shake" -> {
                long slot = (long) (seconds * 20);
                dx = (hash(fx.index, slot, 1) - 0.5f) * 0.8f;
                dy = (hash(fx.index, slot, 2) - 0.5f) * 0.8f;
            }
            case "glitch" -> {
                long slot = (long) (seconds * 10);
                if (hash(fx.index, slot, 3) < 0.03f) {
                    out = out.withObfuscated(true);
                    dx = hash(fx.index, slot, 4) < 0.5f ? -1f : 1f;
                }
            }
            default -> {
            }
        }
        return new Glyph(out, dx, dy);
    }

    private static int color(Fx fx, float pos, float phase, long cycle, double seconds) {
        return switch (fx.effect) {
            // The gradient scrolls through the letters: a to b and back.
            case "flow" -> lerp(fx.a, fx.b, triangle(pos * 0.5f - phase));
            // A bright band sweeps across in the first 60% of the cycle, then rests.
            case "shimmer" -> {
                float sweep = phase / 0.6f;
                if (sweep > 1f) yield fx.a;
                float center = -0.3f + sweep * 1.6f;
                float k = Math.max(0f, 1f - Math.abs(pos - center) / 0.25f);
                yield lerp(fx.a, fx.b, smooth(k));
            }
            // The whole title breathes between the two colors.
            case "pulse" -> lerp(fx.a, fx.b, 0.5f - 0.5f * (float) Math.cos(2 * Math.PI * phase));
            // Hue cycles across the letters.
            case "rainbow" -> hsv(fract(pos * 0.5f - phase), 0.55f, 1f);
            // Some letters flash to the second color once per cycle.
            case "sparkle" -> {
                if (hash(fx.index, cycle, 5) > 0.45f) yield fx.a;
                float at = hash(fx.index, cycle, 6) * 0.85f;
                float k = Math.max(0f, 1f - Math.abs(phase - at) / 0.08f);
                yield lerp(fx.a, fx.b, k);
            }
            // Uneven flicker, like a flame.
            case "ember" -> lerp(fx.a, fx.b, noise(fx.index * 7.31f + (float) (seconds / fx.period) * 3f));
            // "static" and anything unknown: the still gradient.
            default -> lerp(fx.a, fx.b, pos);
        };
    }

    /** A soft shadow in the title's color that breathes with the cycle. ARGB. */
    private static int glow(int rgb, float phase) {
        float k = 0.5f - 0.5f * (float) Math.cos(2 * Math.PI * phase);
        int alpha = (int) (110 + 110 * k);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static Fx parse(String marker) {
        Fx cached = CACHE.get(marker);
        if (cached != null) return cached;

        Fx fx = INVALID;
        try {
            String effect = "static";
            String motion = "none";
            boolean glow = false;
            float period = 4f;
            int a = 0xFFFFFF;
            int b = 0xFFFFFF;
            int index = 0;
            int count = 1;
            for (String part : marker.substring(PREFIX.length()).split(";")) {
                int eq = part.indexOf('=');
                if (eq < 0) continue;
                String key = part.substring(0, eq);
                String value = part.substring(eq + 1);
                switch (key) {
                    case "fx" -> effect = value;
                    case "mo" -> motion = value;
                    case "gl" -> glow = "1".equals(value);
                    case "sp" -> period = Math.max(0.25f, Float.parseFloat(value));
                    case "a" -> a = Integer.parseInt(value, 16);
                    case "b" -> b = Integer.parseInt(value, 16);
                    case "i" -> index = Integer.parseInt(value);
                    case "n" -> count = Math.max(1, Integer.parseInt(value));
                    default -> {
                    }
                }
            }
            fx = new Fx(effect, motion, glow, period, a, b, index, count);
        } catch (RuntimeException e) {
            // Malformed marker: draw the letter as sent.
        }

        if (CACHE.size() > CACHE_LIMIT) CACHE.clear();
        CACHE.put(marker, fx);
        return fx;
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float fract(float x) {
        return x - (float) Math.floor(x);
    }

    /** 0 to 1 and back over one unit. */
    private static float triangle(float x) {
        return 1f - Math.abs(2f * fract(x) - 1f);
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    /** Repeatable pseudo random 0 to 1 for a letter, a time slot, and a salt. */
    private static float hash(int index, long slot, int salt) {
        long h = index * 0x9E3779B97F4A7C15L + slot * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return (h >>> 40) / (float) (1L << 24);
    }

    /** Smooth 1D value noise, 0 to 1. */
    private static float noise(float x) {
        long i = (long) Math.floor(x);
        float f = x - i;
        float u = smooth(f);
        return hash(0, i, 7) * (1f - u) + hash(0, i + 1, 7) * u;
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float x = c * (1f - Math.abs((h * 6f) % 2f - 1f));
        float m = v - c;
        float r;
        float g;
        float b;
        int sector = (int) (h * 6f) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return ((int) ((r + m) * 255) << 16) | ((int) ((g + m) * 255) << 8) | (int) ((b + m) * 255);
    }
}
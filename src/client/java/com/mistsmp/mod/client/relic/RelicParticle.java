package com.mistsmp.mod.client.relic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The two relic particles.
 *
 * Wisp: a pale curl of mist that fades in, drifts upward while slowly swelling, and fades out.
 * Spark: a tiny gold fleck that pops off the item, twinkles, and falls away quickly.
 */
public final class RelicParticle extends SingleQuadParticle {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");

    private enum Kind { WISP, SPARK }

    private final Kind kind;
    private final float maxAlpha;
    private final float baseSize;
    private final float spin;

    private RelicParticle(Kind kind, ClientLevel level, double x, double y, double z,
                          double xd, double yd, double zd, SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, xd, yd, zd, sprites.get(random));
        this.kind = kind;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = false;

        if (kind == Kind.WISP) {
            this.lifetime = 32 + random.nextInt(16);
            this.friction = 0.96F;
            this.gravity = -0.004F;
            this.baseSize = 0.10F + random.nextFloat() * 0.05F;
            this.maxAlpha = 0.45F + random.nextFloat() * 0.15F;
            // Pale mist with the faintest blue, like the server's fog.
            float shade = 0.86F + random.nextFloat() * 0.08F;
            this.setColor(shade, shade + 0.02F, Math.min(1.0F, shade + 0.07F));
            this.spin = (random.nextFloat() - 0.5F) * 0.06F;
        } else {
            this.lifetime = 10 + random.nextInt(8);
            this.friction = 0.9F;
            this.gravity = 0.03F;
            this.baseSize = 0.035F + random.nextFloat() * 0.02F;
            this.maxAlpha = 1.0F;
            // Warm gold, matching the relic glint.
            this.setColor(1.0F, 0.80F + random.nextFloat() * 0.08F, 0.32F + random.nextFloat() * 0.1F);
            this.spin = 0.0F;
        }

        this.quadSize = this.baseSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.setAlpha(kind == Kind.WISP ? 0.0F : this.maxAlpha);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float life = (float) this.age / this.lifetime;
        this.oRoll = this.roll;
        this.roll += this.spin;

        if (kind == Kind.WISP) {
            // Fade in over the first quarter, hold, fade out over the last half. Swell as it rises.
            float fadeIn = Mth.clamp(life / 0.25F, 0.0F, 1.0F);
            float fadeOut = Mth.clamp((1.0F - life) / 0.5F, 0.0F, 1.0F);
            this.setAlpha(this.maxAlpha * Math.min(fadeIn, fadeOut));
            this.quadSize = this.baseSize * (1.0F + life * 0.8F);
            // A slow sideways sway so wisps curl instead of rising in a straight line.
            this.xd += (this.random.nextFloat() - 0.5F) * 0.002F;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.002F;
        } else {
            // Twinkle, then shrink away.
            float twinkle = 0.65F + 0.35F * Mth.sin(this.age * 1.7F);
            this.setAlpha(this.maxAlpha * twinkle * Mth.clamp((1.0F - life) / 0.4F, 0.0F, 1.0F));
            this.quadSize = this.baseSize * (1.0F - life * 0.5F);
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    /** Wisps glow faintly and sparks are full bright, so both read in the dark. */
    @Override
    public int getLightCoords(float partialTick) {
        if (kind == Kind.SPARK) return 0xF000F0;
        int packed = super.getLightCoords(partialTick);
        int block = Math.max((packed >> 4) & 0xF, 10);
        int sky = (packed >> 20) & 0xF;
        return block << 4 | sky << 20;
    }

    public static ParticleProvider<SimpleParticleType> wisp(SpriteSet sprites) {
        return (options, level, x, y, z, xd, yd, zd, random) ->
                hasSprites(sprites) ? new RelicParticle(Kind.WISP, level, x, y, z, xd, yd, zd, sprites, random) : null;
    }

    public static ParticleProvider<SimpleParticleType> spark(SpriteSet sprites) {
        return (options, level, x, y, z, xd, yd, zd, random) ->
                hasSprites(sprites) ? new RelicParticle(Kind.SPARK, level, x, y, z, xd, yd, zd, sprites, random) : null;
    }

    private static boolean warnedMissing;

    /**
     * Vanilla only fills a sprite set when it finds assets/<namespace>/particles/<name>.json in a loaded pack.
     * If that file is missing the set stays empty and asking it for a sprite throws. Skip the particle
     * instead of crashing the game, and log it once so the cause is visible in latest.log.
     */
    private static boolean hasSprites(SpriteSet sprites) {
        try {
            sprites.first();
            return true;
        } catch (RuntimeException e) {
            if (!warnedMissing) {
                warnedMissing = true;
                LOGGER.warn("Relic particle sprites are not loaded; check assets/mistsmp/particles/relic_wisp.json and relic_spark.json are in the mod jar");
            }
            return false;
        }
    }
}
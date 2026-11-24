package com.mistsmp.mod.client.cosmetic.particle;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * One custom cosmetic particle. Everything about how it looks and moves comes from its {@link CustomParticleSpec},
 * so new particles are made in the resource pack, never in code.
 *
 * Motion each tick: vanilla's own (velocity, gravity, drag, and collision when enabled), plus any extra
 * displacement from follow (the player's movement since last tick), orbit, and sway.
 */
final class CosmeticParticle extends SingleQuadParticle {

    private static final float TICKS_PER_SECOND = 20f;

    private final CustomParticleSpec spec;
    private final List<TextureAtlasSprite> frames;
    private final long spawnTick;
    private final Supplier<Vec3> center;
    private Vec3 lastCenter;

    private final float baseSize;
    private final float baseAlpha;
    private final int startColor;
    private final float spinPerTick;

    // Orbit, sway, and pulse state.
    private final float orbitStartAngle;
    private final float swayDirX;
    private final float swayDirZ;
    private final float swayPhase;
    private final float pulsePhase;
    private double lastOrbitX;
    private double lastOrbitY;
    private double lastOrbitZ;
    private double lastSway;

    /**
     * @param center        where to follow and orbit around, read each tick; null when neither is used.
     *                      Returning null stops following (the player left or the follower despawned).
     * @param spawnTick     the level's game time at spawn, for {@link #gone(long)}
     */
    CosmeticParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                     CustomParticleSpec spec, List<TextureAtlasSprite> frames, String tint,
                     Supplier<Vec3> center, long spawnTick, RandomSource random) {
        super(level, x, y, z, xd, yd, zd, frames.get(spec.frameMode() == CustomParticleSpec.FrameMode.RANDOM
                ? random.nextInt(frames.size()) : 0));
        this.spec = spec;
        this.frames = frames;
        this.spawnTick = spawnTick;
        this.center = center;
        this.lastCenter = center == null ? null : center.get();

        // The super constructor adds its own small random velocity; use exactly what the definition asks for.
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = spec.collide();
        this.friction = spec.drag();
        // Vanilla subtracts 0.04 * gravity from the vertical speed each tick.
        this.gravity = spec.gravity() / 0.04f;
        this.lifetime = Math.max(1, Math.round(spec.lifetime().pick(random)));

        this.baseSize = spec.size().pick(random);
        this.baseAlpha = spec.alpha().pick(random);
        this.startColor = pickColor(spec, tint, random);
        this.spinPerTick = spec.spin().pick(random) * Mth.DEG_TO_RAD;
        this.roll = spec.rotation() == null
                ? random.nextFloat() * Mth.TWO_PI
                : spec.rotation().pick(random) * Mth.DEG_TO_RAD;
        this.oRoll = this.roll;

        this.orbitStartAngle = random.nextFloat() * Mth.TWO_PI;
        float swayAngle = random.nextFloat() * Mth.TWO_PI;
        this.swayDirX = Mth.cos(swayAngle);
        this.swayDirZ = Mth.sin(swayAngle);
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        this.pulsePhase = random.nextFloat() * Mth.TWO_PI;

        if (spec.orbit()) {
            // Start on the orbit circle around the center (or the spawn point when not following).
            this.lastOrbitX = Mth.cos(orbitStartAngle) * spec.orbitRadius();
            this.lastOrbitZ = Mth.sin(orbitStartAngle) * spec.orbitRadius();
            Vec3 c = lastCenter != null ? lastCenter : new Vec3(x, y, z);
            this.setPos(c.x + lastOrbitX, y, c.z + lastOrbitZ);
            this.xo = this.x;
            this.zo = this.z;
        }

        applyLook(0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        this.oRoll = this.roll;
        this.roll += this.spinPerTick;

        double dx = 0;
        double dy = 0;
        double dz = 0;

        if (center != null && lastCenter != null) {
            Vec3 now = center.get();
            if (now != null) {
                if (spec.follow()) {
                    dx += now.x - lastCenter.x;
                    dy += now.y - lastCenter.y;
                    dz += now.z - lastCenter.z;
                }
                lastCenter = now;
            }
        }

        if (spec.orbit()) {
            float angle = orbitStartAngle + this.age * spec.orbitSpeed() * Mth.DEG_TO_RAD;
            double ox = Mth.cos(angle) * spec.orbitRadius();
            double oy = this.age * spec.orbitRise();
            double oz = Mth.sin(angle) * spec.orbitRadius();
            dx += ox - lastOrbitX;
            dy += oy - lastOrbitY;
            dz += oz - lastOrbitZ;
            lastOrbitX = ox;
            lastOrbitY = oy;
            lastOrbitZ = oz;
        }

        if (spec.swayAmount() > 0f) {
            double sway = spec.swayAmount()
                    * Mth.sin(swayPhase + this.age * spec.swaySpeed() * Mth.TWO_PI / TICKS_PER_SECOND);
            double delta = sway - lastSway;
            dx += delta * swayDirX;
            dz += delta * swayDirZ;
            lastSway = sway;
        }

        if (dx != 0 || dy != 0 || dz != 0) {
            this.move(dx, dy, dz);
        }

        if (frames.size() > 1) {
            int index = switch (spec.frameMode()) {
                case LIFE -> Math.min(frames.size() - 1, this.age * frames.size() / this.lifetime);
                case LOOP -> (this.age / spec.frameTicks()) % frames.size();
                case RANDOM -> -1;
            };
            if (index >= 0) this.setSprite(frames.get(index));
        }

        applyLook((float) this.age / this.lifetime);
    }

    /** Size, color, and opacity for this point in the particle's life (0 to 1). */
    private void applyLook(float life) {
        float size = baseSize * Mth.lerp(life, 1f, spec.sizeEnd());
        if (spec.pulseAmount() > 0f) {
            size *= 1f + spec.pulseAmount()
                    * Mth.sin(pulsePhase + this.age * spec.pulseSpeed() * Mth.TWO_PI / TICKS_PER_SECOND);
        }
        this.quadSize = Math.max(0f, size);

        int rgb = spec.colorEnd() == null ? startColor : lerpColor(startColor, spec.colorEnd(), life);
        this.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);

        float fade = 1f;
        if (spec.fadeIn() > 0f) fade = Math.min(fade, life / spec.fadeIn());
        if (spec.fadeOut() > 0f) fade = Math.min(fade, (1f - life) / spec.fadeOut());
        this.setAlpha(baseAlpha * Mth.clamp(fade, 0f, 1f));
    }

    /**
     * True once the particle has ended. Also true once its lifetime has clearly passed, because the game drops
     * particles without removing them one by one (changing dimension, for example), and a particle dropped that
     * way would otherwise count against maxAlive forever.
     */
    boolean gone(long now) {
        return !this.isAlive() || now - spawnTick > this.lifetime + 5L;
    }

    @Override
    protected Layer getLayer() {
        return spec.cutout() ? Layer.OPAQUE : Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return spec.glow() ? 0xF000F0 : super.getLightCoords(partialTick);
    }

    private static int pickColor(CustomParticleSpec spec, String tint, RandomSource random) {
        if (spec.useTint()) {
            Integer parsed = ParticleSpec.parseColor(tint);
            return parsed != null ? parsed : 0xFFFFFF;
        }
        if (spec.colorA() == null) return 0xFFFFFF;
        if (spec.colorB() == null) return spec.colorA();
        return lerpColor(spec.colorA(), spec.colorB(), random.nextFloat());
    }

    private static int lerpColor(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }
}

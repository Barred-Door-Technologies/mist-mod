package com.mistsmp.mod.client.cosmetic.particle;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mistsmp.mod.client.config.MistSettings;
import com.mistsmp.mod.client.cosmetic.LoadoutCache;
import com.mistsmp.mod.client.cosmetic.def.CosmeticAnimator;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.follower.FollowerTracker;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;
import com.mistsmp.mod.cosmetic.EquippedCosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns cosmetic particles on this client, once per tick, for AURA and FOLLOWER cosmetics.
 * No packets and no server load: every client spawns the particles it sees.
 *
 * Skipped: players further than 48 blocks, invisible players, spectators, other players while
 * the K toggle is off, and your own particles in first person (they would fill your screen).
 * Vanilla's Particles setting (All / Decreased / Minimal) still applies on top: vanilla checks it for
 * vanilla particle types, and this class checks it for custom ones, which skip vanilla's spawn path.
 *
 * Spawn shapes (around the area's center for auras, or the follower):
 *   random  the original spread for each area (body box, feet ring, head halo)
 *   ring    evenly around a flat circle of radius (player half width + spread) at the area's height
 *   sphere  on the surface of a ball of radius (player half width + spread) around the area's center
 *   column  around a circle like ring, at any height from the feet to the head
 */
public final class CosmeticParticles {

    private static final double MAX_DISTANCE = 48.0;
    private static final CosmeticSlot[] SLOTS = {CosmeticSlot.AURA, CosmeticSlot.FOLLOWER};

    /** Parsed particle lists, rebuilt when a definition is reloaded (definitions are replaced, not edited). */
    private static final Map<CosmeticDefinition, List<ParticleSpec>> SPECS = new HashMap<>();

    /** Live custom particles per player per entry, for maxAlive. Ended ones are pruned before each spawn. */
    private static final Map<UUID, Map<ParticleSpec, ArrayDeque<CosmeticParticle>>> ALIVE = new HashMap<>();
    private static ClientLevel lastLevel;

    private CosmeticParticles() {
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused() || !ClientSession.isWelcomed()) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.mainCamera().position();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        boolean showOthers = MistSettings.get().showOtherPlayersParticles();
        ParticleStatus status = minecraft.options.particles().get();
        if (level != lastLevel || ALIVE.size() > 256) {
            ALIVE.clear();
            lastLevel = level;
        }

        for (AbstractClientPlayer player : level.players()) {
            boolean self = player == minecraft.player;
            if (self ? firstPerson : !showOthers) continue;
            if (player.isInvisible() || player.isSpectator()) continue;
            if (player.position().distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;

            for (CosmeticSlot slot : SLOTS) {
                EquippedCosmetic equipped = LoadoutCache.getEquipped(player.getUUID(), slot);
                if (equipped == null) continue;

                Vec3 anchor = null;
                if (slot == CosmeticSlot.FOLLOWER) {
                    anchor = FollowerTracker.positionOf(player.getUUID());
                    if (anchor == null) continue;
                }

                CosmeticDefinition definition = CosmeticDefinitions.get(equipped.id());
                for (ParticleSpec spec : specsFor(definition)) {
                    if (!CosmeticAnimator.conditionMet(player, spec.when())) continue;
                    int count = rollCount(spec.perSecond(), level.getRandom()) * spec.burst();
                    for (int i = 0; i < count; i++) {
                        spawn(minecraft, level, player, slot, anchor, spec, equipped.param("tint"), status);
                    }
                }
            }
        }
    }

    private static List<ParticleSpec> specsFor(CosmeticDefinition definition) {
        if (SPECS.size() > 256) SPECS.clear();
        return SPECS.computeIfAbsent(definition, ParticleSpec::listFrom);
    }

    /** perSecond / 20 per tick, with the fraction rolled so low rates still spawn on average. */
    private static int rollCount(float perSecond, RandomSource random) {
        float perTick = perSecond / 20f;
        int whole = (int) perTick;
        return whole + (random.nextFloat() < perTick - whole ? 1 : 0);
    }

    private static void spawn(Minecraft minecraft, ClientLevel level, AbstractClientPlayer player, CosmeticSlot slot,
                              Vec3 anchor, ParticleSpec spec, String tint, ParticleStatus status) {
        RandomSource random = level.getRandom();
        Vec3 position = spawnPosition(player, anchor, spec, random);

        if (spec.custom() == null) {
            level.addParticle(spec.options(tint), position.x, position.y, position.z,
                    spec.velocityX(), spec.velocityY(), spec.velocityZ());
            return;
        }
        spawnCustom(minecraft, level, player, slot, anchor, spec, tint, status, position, random);
    }

    private static void spawnCustom(Minecraft minecraft, ClientLevel level, AbstractClientPlayer player,
                                    CosmeticSlot slot, Vec3 anchor, ParticleSpec spec, String tint,
                                    ParticleStatus status, Vec3 position, RandomSource random) {
        // Same thinning vanilla applies to its own particles: Decreased drops about a third, Minimal drops all.
        if (status == ParticleStatus.MINIMAL) return;
        if (status == ParticleStatus.DECREASED && random.nextInt(3) == 0) return;

        CustomParticleSpec custom = spec.custom();
        long now = level.getGameTime();
        ArrayDeque<CosmeticParticle> alive = ALIVE.computeIfAbsent(player.getUUID(), id -> new IdentityHashMap<>())
                .computeIfAbsent(spec, s -> new ArrayDeque<>());
        alive.removeIf(p -> p.gone(now));
        if (alive.size() >= custom.maxAlive()) return;

        List<TextureAtlasSprite> frames = ParticleSprites.resolve(custom.textures());
        if (frames.isEmpty()) return;

        Supplier<Vec3> center = null;
        if (custom.follow() || custom.orbit()) {
            UUID id = player.getUUID();
            center = slot == CosmeticSlot.FOLLOWER
                    ? () -> FollowerTracker.positionOf(id)
                    : () -> player.isRemoved() ? null : areaCenter(player, spec.area());
        }

        double xd = spec.velocityX() + (random.nextDouble() * 2 - 1) * custom.velocityRandomX();
        double yd = spec.velocityY() + (random.nextDouble() * 2 - 1) * custom.velocityRandomY();
        double zd = spec.velocityZ() + (random.nextDouble() * 2 - 1) * custom.velocityRandomZ();

        CosmeticParticle particle = new CosmeticParticle(level, position.x, position.y, position.z, xd, yd, zd,
                custom, frames, tint, center, now, random);
        alive.add(particle);
        minecraft.particleEngine.add(particle);
    }

    /** Where an aura's area is centered: the middle of the body, the feet, or just above the head. */
    private static Vec3 areaCenter(AbstractClientPlayer player, ParticleSpec.Area area) {
        return switch (area) {
            case FEET -> new Vec3(player.getX(), player.getY() + 0.1, player.getZ());
            case HEAD -> new Vec3(player.getX(), player.getY() + player.getBbHeight() + 0.15, player.getZ());
            case BODY -> new Vec3(player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ());
        };
    }

    private static Vec3 spawnPosition(AbstractClientPlayer player, Vec3 anchor, ParticleSpec spec, RandomSource random) {
        double spread = spec.spread();
        double x;
        double y;
        double z;

        if (anchor != null) {
            // Follower: around the follower.
            double radius = spread;
            switch (spec.shape()) {
                case RING -> {
                    double angle = random.nextDouble() * Math.PI * 2;
                    x = anchor.x + Math.cos(angle) * radius;
                    y = anchor.y;
                    z = anchor.z + Math.sin(angle) * radius;
                }
                case SPHERE -> {
                    Vec3 dir = randomDirection(random);
                    x = anchor.x + dir.x * radius;
                    y = anchor.y + dir.y * radius;
                    z = anchor.z + dir.z * radius;
                }
                case COLUMN -> {
                    double angle = random.nextDouble() * Math.PI * 2;
                    x = anchor.x + Math.cos(angle) * radius;
                    y = anchor.y + (random.nextDouble() * 2 - 1) * radius;
                    z = anchor.z + Math.sin(angle) * radius;
                }
                default -> {
                    x = anchor.x + (random.nextDouble() * 2 - 1) * spread;
                    y = anchor.y + (random.nextDouble() * 2 - 1) * spread;
                    z = anchor.z + (random.nextDouble() * 2 - 1) * spread;
                }
            }
            return new Vec3(x + spec.offsetX(), y + spec.offsetY(), z + spec.offsetZ());
        }

        double halfWidth = player.getBbWidth() / 2.0;
        Vec3 center = areaCenter(player, spec.area());
        switch (spec.shape()) {
            case RING -> {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = halfWidth + spread;
                x = center.x + Math.cos(angle) * radius;
                y = center.y;
                z = center.z + Math.sin(angle) * radius;
            }
            case SPHERE -> {
                Vec3 dir = randomDirection(random);
                double radius = halfWidth + spread;
                x = center.x + dir.x * radius;
                y = center.y + dir.y * radius;
                z = center.z + dir.z * radius;
            }
            case COLUMN -> {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = halfWidth + spread;
                x = player.getX() + Math.cos(angle) * radius;
                y = player.getY() + random.nextDouble() * player.getBbHeight();
                z = player.getZ() + Math.sin(angle) * radius;
            }
            default -> {
                switch (spec.area()) {
                    case FEET -> {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double radius = halfWidth + spread;
                        x = player.getX() + Math.cos(angle) * radius;
                        y = player.getY() + 0.05 + random.nextDouble() * 0.1;
                        z = player.getZ() + Math.sin(angle) * radius;
                    }
                    case HEAD -> {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double radius = 0.25 + spread * random.nextDouble();
                        x = player.getX() + Math.cos(angle) * radius;
                        y = player.getY() + player.getBbHeight() + 0.15;
                        z = player.getZ() + Math.sin(angle) * radius;
                    }
                    default -> {
                        double reach = halfWidth + spread;
                        x = player.getX() + (random.nextDouble() * 2 - 1) * reach;
                        y = player.getY() + random.nextDouble() * player.getBbHeight();
                        z = player.getZ() + (random.nextDouble() * 2 - 1) * reach;
                    }
                }
            }
        }
        return new Vec3(x + spec.offsetX(), y + spec.offsetY(), z + spec.offsetZ());
    }

    private static Vec3 randomDirection(RandomSource random) {
        double yDir = random.nextDouble() * 2 - 1;
        double angle = random.nextDouble() * Math.PI * 2;
        double flat = Math.sqrt(1 - yDir * yDir);
        return new Vec3(Math.cos(angle) * flat, yDir, Math.sin(angle) * flat);
    }
}

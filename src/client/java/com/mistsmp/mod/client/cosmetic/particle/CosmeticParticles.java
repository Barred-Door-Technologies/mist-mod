package com.mistsmp.mod.client.cosmetic.particle;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mistsmp.mod.client.config.MistSettings;
import com.mistsmp.mod.client.cosmetic.BackCosmetics;
import com.mistsmp.mod.client.cosmetic.CosmeticVisibility;
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
 * Spawns cosmetic particles on this client, once per tick, for every slot whose definition has a
 * "particles" list. No packets and no server load: every client spawns the particles it sees.
 *
 * A slot's particles spawn only while its cosmetic is showing (CosmeticVisibility): a hat's while a
 * helmet is worn, wings' while an elytra is worn, and so on. FACE, FOLLOWER, and AURA always show.
 *
 * Skipped: players further than 48 blocks, invisible players, spectators, other players while
 * the K toggle is off, and your own particles in first person (they would fill your screen).
 * Vanilla's Particles setting (All / Decreased / Minimal) still applies on top: vanilla checks it for
 * vanilla particle types, and this class checks it for custom ones, which skip vanilla's spawn path.
 *
 * Areas. body, feet, and head are centered on the player and ignore which way they face. The others
 * are placed on the body and turn with it: face, back, wings (across the wing span behind the
 * shoulders), legs, and hand (the hand holding a HELD cosmetic's item, otherwise the main hand).
 * While gliding with an elytra the body lies along the look direction, and these areas tilt with it.
 * An entry with no area uses its slot's default (defaultArea).
 *
 * Spawn shapes:
 *   random  the area's own spread (body box, feet ring, head halo, wing span, and so on)
 *   ring    evenly around a flat circle at the area's center; radius (player half width + spread)
 *           for body, feet, and head, and spread for the other areas and followers
 *   sphere  on the surface of a ball around the area's center, same radius as ring
 *   column  body, feet, and head: around the player at any height from the feet to the head;
 *           other areas and followers: around a circle of radius spread, up to spread above or below
 *
 * offset is relative to the player's body: [right, up, forward], in blocks.
 */
public final class CosmeticParticles {

    private static final double MAX_DISTANCE = 48.0;
    private static final CosmeticSlot[] SLOTS = CosmeticSlot.values();

    /** Body measurements below are for a standing 1.8-block player and scale with the hitbox (crouching). */
    private static final double STANDING_HEIGHT = 1.8;
    private static final Vec3 UP = new Vec3(0, 1, 0);

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

                CosmeticDefinition definition = CosmeticDefinitions.get(equipped.id());
                List<ParticleSpec> specs = specsFor(definition);
                if (specs.isEmpty()) continue;
                if (!CosmeticVisibility.isShowing(player, slot, definition)) continue;

                Vec3 anchor = null;
                if (slot == CosmeticSlot.FOLLOWER) {
                    anchor = FollowerTracker.positionOf(player.getUUID());
                    if (anchor == null) continue;
                }

                double side = slot == CosmeticSlot.HELD
                        ? CosmeticVisibility.heldSide(player, definition)
                        : CosmeticVisibility.mainHandSide(player);

                for (ParticleSpec spec : specs) {
                    if (!CosmeticAnimator.conditionMet(player, spec.when())) continue;
                    ParticleSpec.Area area = spec.area() != null ? spec.area() : defaultArea(slot, definition);
                    int count = rollCount(spec.perSecond(), level.getRandom()) * spec.burst();
                    for (int i = 0; i < count; i++) {
                        spawn(minecraft, level, player, slot, anchor, spec, area, side, equipped.param("tint"), status);
                    }
                }
            }
        }
    }

    /** The area an entry uses when it names none: where the slot's cosmetic sits on the player. */
    private static ParticleSpec.Area defaultArea(CosmeticSlot slot, CosmeticDefinition definition) {
        return switch (slot) {
            case HEAD -> ParticleSpec.Area.HEAD;
            case FACE -> ParticleSpec.Area.FACE;
            case LEGS -> ParticleSpec.Area.LEGS;
            case FEET -> ParticleSpec.Area.FEET;
            case HELD, HANDS -> ParticleSpec.Area.HAND;
            case BACK -> BackCosmetics.Style.of(definition) == BackCosmetics.Style.CAPE
                    ? ParticleSpec.Area.BACK
                    : ParticleSpec.Area.WINGS;
            // FOLLOWER spawns around the follower and never reads the area
            case CHEST, AURA, FOLLOWER -> ParticleSpec.Area.BODY;
        };
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
                              Vec3 anchor, ParticleSpec spec, ParticleSpec.Area area, double side, String tint,
                              ParticleStatus status) {
        RandomSource random = level.getRandom();
        Vec3 position = spawnPosition(player, anchor, spec, area, side, random);

        if (spec.custom() == null) {
            level.addParticle(spec.options(tint), position.x, position.y, position.z,
                    spec.velocityX(), spec.velocityY(), spec.velocityZ());
            return;
        }
        spawnCustom(minecraft, level, player, slot, spec, area, side, tint, status, position, random);
    }

    private static void spawnCustom(Minecraft minecraft, ClientLevel level, AbstractClientPlayer player,
                                    CosmeticSlot slot, ParticleSpec spec, ParticleSpec.Area area, double side,
                                    String tint, ParticleStatus status, Vec3 position, RandomSource random) {
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
                    : () -> player.isRemoved() ? null : areaCenter(player, area, side);
        }

        double xd = spec.velocityX() + (random.nextDouble() * 2 - 1) * custom.velocityRandomX();
        double yd = spec.velocityY() + (random.nextDouble() * 2 - 1) * custom.velocityRandomY();
        double zd = spec.velocityZ() + (random.nextDouble() * 2 - 1) * custom.velocityRandomZ();

        CosmeticParticle particle = new CosmeticParticle(level, position.x, position.y, position.z, xd, yd, zd,
                custom, frames, tint, center, now, random);
        alive.add(particle);
        minecraft.particleEngine.add(particle);
    }

    /** True for the areas centered on the player that ignore which way they face. */
    private static boolean isRoundArea(ParticleSpec.Area area) {
        return area == ParticleSpec.Area.BODY || area == ParticleSpec.Area.FEET || area == ParticleSpec.Area.HEAD;
    }

    /** Where an area is centered, for shapes and for follow / orbit. */
    private static Vec3 areaCenter(AbstractClientPlayer player, ParticleSpec.Area area, double side) {
        return switch (area) {
            case FEET -> new Vec3(player.getX(), player.getY() + 0.1, player.getZ());
            case HEAD -> new Vec3(player.getX(), player.getY() + player.getBbHeight() + 0.15, player.getZ());
            case BODY -> new Vec3(player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ());
            case FACE -> Frame.of(player).point(0, 1.62, 0.3);
            case BACK -> Frame.of(player).point(0, 1.1, -0.3);
            case WINGS -> Frame.of(player).point(0, 0.85, -0.3);
            case LEGS -> Frame.of(player).point(0, 0.4, 0);
            case HAND -> Frame.of(player).point(0.38 * side, 0.76, 0.12);
        };
    }

    private static Vec3 spawnPosition(AbstractClientPlayer player, Vec3 anchor, ParticleSpec spec,
                                      ParticleSpec.Area area, double side, RandomSource random) {
        Frame frame = Frame.of(player);
        Vec3 offset = frame.direction(spec.offsetX(), spec.offsetY(), spec.offsetZ());

        if (anchor != null) {
            // Follower: around the follower.
            return aroundPoint(anchor, spec.shape(), spec.spread(), random).add(offset);
        }
        if (!isRoundArea(area)) {
            Vec3 position = spec.shape() == ParticleSpec.Shape.RANDOM
                    ? randomOnBody(frame, area, side, spec.spread(), random)
                    : aroundPoint(areaCenter(player, area, side), spec.shape(), spec.spread(), random);
            return position.add(offset);
        }

        double spread = spec.spread();
        double halfWidth = player.getBbWidth() / 2.0;
        Vec3 center = areaCenter(player, area, side);
        double x;
        double y;
        double z;
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
                switch (area) {
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
        return new Vec3(x, y, z).add(offset);
    }

    /**
     * The "random" shape for the body areas, in body measurements (right, height from the feet, forward).
     * Wings match a folded elytra: about 0.7 blocks out to each side, from the shoulders to the knees.
     */
    private static Vec3 randomOnBody(Frame frame, ParticleSpec.Area area, double side, double spread, RandomSource random) {
        return switch (area) {
            case FACE -> frame.point(symmetric(0.2 + spread, random), 1.5 + random.nextDouble() * 0.3,
                    0.3 + random.nextDouble() * spread);
            case BACK -> frame.point(symmetric(0.25 + spread, random), 0.75 + random.nextDouble() * 0.7,
                    -(0.2 + random.nextDouble() * spread));
            case WINGS -> frame.point(symmetric(0.7 + spread, random), 0.25 + random.nextDouble() * 1.2,
                    -(0.2 + random.nextDouble() * spread));
            case LEGS -> frame.point(symmetric(0.2 + spread, random), 0.05 + random.nextDouble() * 0.7,
                    symmetric(0.15 + spread, random));
            case HAND -> frame.point(0.38 * side + symmetric(spread, random), 0.76 + symmetric(spread, random),
                    0.12 + symmetric(spread, random));
            // round areas never reach here
            case BODY, FEET, HEAD -> frame.point(0, 0.9, 0);
        };
    }

    /** ring, sphere, column, or a random cloud of radius spread around a point (followers and body areas). */
    private static Vec3 aroundPoint(Vec3 point, ParticleSpec.Shape shape, double spread, RandomSource random) {
        return switch (shape) {
            case RING -> {
                double angle = random.nextDouble() * Math.PI * 2;
                yield new Vec3(point.x + Math.cos(angle) * spread, point.y, point.z + Math.sin(angle) * spread);
            }
            case SPHERE -> point.add(randomDirection(random).scale(spread));
            case COLUMN -> {
                double angle = random.nextDouble() * Math.PI * 2;
                yield new Vec3(point.x + Math.cos(angle) * spread,
                        point.y + symmetric(spread, random),
                        point.z + Math.sin(angle) * spread);
            }
            case RANDOM -> new Vec3(point.x + symmetric(spread, random),
                    point.y + symmetric(spread, random),
                    point.z + symmetric(spread, random));
        };
    }

    private static double symmetric(double reach, RandomSource random) {
        return (random.nextDouble() * 2 - 1) * reach;
    }

    private static Vec3 randomDirection(RandomSource random) {
        double yDir = random.nextDouble() * 2 - 1;
        double angle = random.nextDouble() * Math.PI * 2;
        double flat = Math.sqrt(1 - yDir * yDir);
        return new Vec3(Math.cos(angle) * flat, yDir, Math.sin(angle) * flat);
    }

    /**
     * The player's body axes, for placing particles in body measurements (right, height from the feet,
     * forward; blocks, for a standing 1.8-block player).
     *
     * Standing: turns with yBodyRot and scales with the hitbox (crouching). Gliding: the body lies along
     * the look direction, like vanilla's elytra pose, pivoting around the middle of the hitbox.
     */
    private record Frame(Vec3 origin, Vec3 right, Vec3 up, Vec3 forward, double scale, double pivotHeight) {

        static Frame of(AbstractClientPlayer player) {
            double yaw = Math.toRadians(player.yBodyRot);
            // Same axes as RelicParticles: the player's right-hand side and the direction they face.
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            Vec3 facing = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

            if (!player.isFallFlying()) {
                return new Frame(player.position(), right, UP, facing, player.getBbHeight() / STANDING_HEIGHT, 0);
            }
            // Head points along the look direction; the chest faces down (level flight) or back (diving).
            double pitch = Math.toRadians(player.getXRot());
            Vec3 up = facing.scale(Math.cos(pitch)).add(0, -Math.sin(pitch), 0);
            Vec3 forward = facing.scale(-Math.sin(pitch)).add(0, -Math.cos(pitch), 0);
            Vec3 middle = player.position().add(0, player.getBbHeight() / 2.0, 0);
            return new Frame(middle, right, up, forward, 1.0, STANDING_HEIGHT / 2.0);
        }

        Vec3 point(double rightBlocks, double height, double forwardBlocks) {
            return origin.add(direction(rightBlocks, height * scale - pivotHeight, forwardBlocks));
        }

        Vec3 direction(double rightBlocks, double upBlocks, double forwardBlocks) {
            return right.scale(rightBlocks).add(up.scale(upBlocks)).add(forward.scale(forwardBlocks));
        }
    }
}
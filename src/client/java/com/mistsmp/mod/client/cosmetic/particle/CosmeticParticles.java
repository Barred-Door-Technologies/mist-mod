package com.mistsmp.mod.client.cosmetic.particle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns cosmetic particles on this client, once per tick, for AURA and FOLLOWER cosmetics.
 * No packets and no server load: every client spawns the particles it sees.
 *
 * Skipped: players further than 48 blocks, invisible players, spectators, other players while
 * the K toggle is off, and your own particles in first person (they would fill your screen).
 * Vanilla's Particles setting (All / Decreased / Minimal) still applies on top.
 */
public final class CosmeticParticles {

    private static final double MAX_DISTANCE = 48.0;
    private static final CosmeticSlot[] SLOTS = {CosmeticSlot.AURA, CosmeticSlot.FOLLOWER};

    /** Parsed particle lists, rebuilt when a definition is reloaded (definitions are replaced, not edited). */
    private static final Map<CosmeticDefinition, List<ParticleSpec>> SPECS = new HashMap<>();

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
                    int count = rollCount(spec.perSecond(), level.getRandom());
                    for (int i = 0; i < count; i++) {
                        spawn(level, player, anchor, spec, equipped.param("tint"));
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

    private static void spawn(ClientLevel level, AbstractClientPlayer player, Vec3 anchor, ParticleSpec spec, String tint) {
        RandomSource random = level.getRandom();
        double spread = spec.spread();
        double x;
        double y;
        double z;

        if (anchor != null) {
            // Follower: a small cloud around the follower.
            x = anchor.x + (random.nextDouble() * 2 - 1) * spread;
            y = anchor.y + (random.nextDouble() * 2 - 1) * spread;
            z = anchor.z + (random.nextDouble() * 2 - 1) * spread;
        } else {
            double halfWidth = player.getBbWidth() / 2.0;
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

        level.addParticle(spec.options(tint), x, y, z, spec.velocityX(), spec.velocityY(), spec.velocityZ());
    }
}
package com.mistsmp.mod.client.cosmetic.follower;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mistsmp.mod.client.cosmetic.CosmeticModels;
import com.mistsmp.mod.client.cosmetic.LoadoutCache;
import com.mistsmp.mod.client.cosmetic.def.CosmeticAnimator;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Moves every player's follower once per client tick. Followers are simulated on each client
 * (no server load, no packets), so they look the same for everyone without being exactly in sync.
 *
 * Following: the follower eases toward a point behind the player's shoulder.
 * Idle: after orbitWhenIdle.afterTicks without moving, it circles the player instead.
 * Teleports and big gaps snap it straight to the player.
 */
public final class FollowerTracker {

    /** Further than this from its target (blocks) and the follower jumps instead of flying across the map. */
    private static final double SNAP_DISTANCE = 8.0;

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static ClientLevel lastLevel;

    private FollowerTracker() {
    }

    /** Per-player follower state. Render fields are filled by FollowerRenderer each frame. */
    static final class State {
        Identifier cosmetic;
        CosmeticDefinition definition;
        FollowerSettings settings;

        Vec3 position;
        Vec3 previousPosition;
        float yaw;
        float previousYaw;
        int idleTicks;
        boolean orbiting;
        float orbitAngle;

        // Filled during render extraction, read during submit, same frame.
        final ItemStackRenderState item = new ItemStackRenderState();
        boolean visible;
        double renderX;
        double renderY;
        double renderZ;
        float renderYaw;
        int light;
        CosmeticTransform transform;
        AbstractClientPlayer owner;
    }

    public static void tick(ClientLevel level) {
        if (level != lastLevel) {
            STATES.clear();
            lastLevel = level;
        }
        if (level == null || !ClientSession.isWelcomed()) {
            STATES.clear();
            return;
        }

        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            Identifier cosmetic = LoadoutCache.get(player.getUUID(), CosmeticSlot.FOLLOWER);
            if (cosmetic == null || player.isSpectator() || !CosmeticModels.exists(cosmetic)) {
                continue;
            }
            seen.add(player.getUUID());
            State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
            state.owner = player;

            CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
            if (!cosmetic.equals(state.cosmetic) || definition != state.definition) {
                state.cosmetic = cosmetic;
                state.definition = definition;
                state.settings = FollowerSettings.from(definition);
            }
            step(state, player);
        }
        STATES.keySet().retainAll(seen);
    }

    private static void step(State state, AbstractClientPlayer player) {
        FollowerSettings s = state.settings;
        Vec3 feet = player.position();

        boolean moving = CosmeticAnimator.isMoving(player);
        state.idleTicks = moving ? 0 : state.idleTicks + 1;
        boolean shouldOrbit = s.orbits() && state.idleTicks >= s.orbitAfterTicks();

        if (shouldOrbit && !state.orbiting && state.position != null) {
            // Start the circle from wherever the follower is now, so it doesn't jump.
            state.orbitAngle = (float) Math.toDegrees(Math.atan2(state.position.z - feet.z, state.position.x - feet.x));
        }
        state.orbiting = shouldOrbit;

        Vec3 target;
        float targetYaw;
        if (state.orbiting) {
            state.orbitAngle = Mth.wrapDegrees(state.orbitAngle + s.orbitDegreesPerSecond() / 20f);
            double radians = Math.toRadians(state.orbitAngle);
            target = feet.add(Math.cos(radians) * s.orbitRadius(), s.height(), Math.sin(radians) * s.orbitRadius());
            // Face the way it is travelling around the circle.
            targetYaw = state.orbitAngle + (s.orbitDegreesPerSecond() >= 0 ? 0f : 180f);
        } else {
            float bodyYaw = player.yBodyRot;
            double radians = Math.toRadians(bodyYaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            double rightX = -Math.cos(radians);
            double rightZ = -Math.sin(radians);
            target = feet.add(
                    -forwardX * s.distance() + rightX * s.side(),
                    s.height(),
                    -forwardZ * s.distance() + rightZ * s.side());
            targetYaw = bodyYaw;
        }

        if (state.position == null || state.position.distanceToSqr(target) > SNAP_DISTANCE * SNAP_DISTANCE) {
            state.position = target;
            state.previousPosition = target;
            state.yaw = targetYaw;
            state.previousYaw = targetYaw;
            return;
        }

        state.previousPosition = state.position;
        state.previousYaw = state.yaw;
        state.position = state.position.lerp(target, s.smoothing());
        state.yaw = Mth.rotLerp(Math.min(1f, s.smoothing() * 2f), state.yaw, targetYaw);
    }

    /** @return where this player's follower is right now (for particles), or null if they have none. */
    public static Vec3 positionOf(UUID player) {
        State state = STATES.get(player);
        return state == null ? null : state.position;
    }

    static Iterable<State> states() {
        return STATES.values();
    }

    public static void clear() {
        STATES.clear();
        lastLevel = null;
    }
}
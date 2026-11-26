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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Moves every player's follower once per client tick. Followers are simulated on each client
 * (no server load, no packets), so they look the same for everyone without being exactly in sync.
 *
 * Flying (default):
 *   Following: the follower eases toward a point behind the player's shoulder.
 *   Idle: after orbitWhenIdle.afterTicks without moving, it circles the player instead.
 *   Teleports and big gaps snap it straight to the player.
 *
 * Walking (like a tamed wolf or cat):
 *   Stays put while the player is within startDistance; walks after them past it and stops at
 *   stopDistance, faster the further behind it is. Stands on the ground: steps up one block, drops down
 *   ledges, stands on water. No pathfinding: a wall it can't step over stops it, and if it stays stuck
 *   while the player is out of range, or the player gets past teleportDistance, it appears behind them.
 *   Idle: after wanderWhenIdle.afterTicks, it strolls to random spots near the player.
 *
 * state.position is always the model's center (what the renderer draws at). A walking follower's
 * model stands on y = 0, so its center is half a block (times scale) above the ground.
 */
public final class FollowerTracker {

    /** Further than this from its target (blocks) and a flying follower jumps instead of flying across the map. */
    private static final double SNAP_DISTANCE = 8.0;

    /** Highest step a walking follower climbs without help (a full block, like a wolf). */
    private static final double MAX_STEP_UP = 1.0;
    /** How far below it looks for ground before treating the spot as a drop. */
    private static final double MAX_GROUND_SCAN = 6.0;
    /** Clear space it needs above the ground it steps onto. */
    private static final double HEADROOM = 0.5;
    /** Height change per tick when stepping up and when falling. */
    private static final double STEP_UP_SPEED = 0.5;
    private static final double FALL_SPEED = 0.6;
    /** Ticks blocked by a wall, while the player is out of range, before it teleports to them. */
    private static final int STUCK_TELEPORT_TICKS = 40;
    /** Wandering pauses between strolls, in ticks. */
    private static final int WANDER_PAUSE_MIN = 40;
    private static final int WANDER_PAUSE_MAX = 120;
    /** Horizontal movement per tick (blocks, squared) that counts as walking, for animation. */
    private static final double WALKING_SQR = 0.0004;

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

        // Walking only.
        double groundY;
        boolean following;
        boolean walking;
        int stuckTicks;
        Vec3 wanderTarget;
        int wanderPause;

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
                boolean movementChanged = state.settings != null
                        && state.settings.movement() != FollowerSettings.from(definition).movement();
                state.cosmetic = cosmetic;
                state.definition = definition;
                state.settings = FollowerSettings.from(definition);
                // Switching between flying and walking: start over next to the player.
                if (movementChanged) state.position = null;
            }

            boolean moving = CosmeticAnimator.isMoving(player);
            state.idleTicks = moving ? 0 : state.idleTicks + 1;

            if (state.settings.walks()) {
                stepWalking(state, player, level);
            } else {
                state.walking = false;
                stepFlying(state, player);
            }
        }
        STATES.keySet().retainAll(seen);
    }

    // ---------------------------------------------------------------- flying

    private static void stepFlying(State state, AbstractClientPlayer player) {
        FollowerSettings s = state.settings;
        Vec3 feet = player.position();

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
            target = behind(player, s).add(0, s.height(), 0);
            targetYaw = player.yBodyRot;
        }

        if (state.position == null || state.position.distanceToSqr(target) > SNAP_DISTANCE * SNAP_DISTANCE) {
            place(state, target, targetYaw);
            return;
        }

        state.previousPosition = state.position;
        state.previousYaw = state.yaw;
        state.position = state.position.lerp(target, s.smoothing());
        state.yaw = Mth.rotLerp(Math.min(1f, s.smoothing() * 2f), state.yaw, targetYaw);
    }

    // ---------------------------------------------------------------- walking

    private static void stepWalking(State state, AbstractClientPlayer player, Level level) {
        FollowerSettings s = state.settings;
        double centerLift = 0.5 * s.scale();
        Vec3 owner = player.position();

        if (state.position == null) {
            teleportBehind(state, player, level, centerLift);
            return;
        }

        double dx = owner.x - state.position.x;
        double dz = owner.z - state.position.z;
        double gap = Math.sqrt(dx * dx + dz * dz);
        double heightGap = Math.abs(owner.y - state.groundY);

        if (gap > s.teleportDistance() || heightGap > s.teleportDistance()
                || (state.stuckTicks >= STUCK_TELEPORT_TICKS && gap > s.startDistance())) {
            teleportBehind(state, player, level, centerLift);
            return;
        }

        // Following with hysteresis, like a wolf: start past startDistance, stop inside stopDistance.
        if (!state.following && gap > s.startDistance()) state.following = true;
        if (state.following && gap < s.stopDistance()) state.following = false;

        Vec3 goal = null;
        double speed = 0;
        if (state.following) {
            state.wanderTarget = null;
            goal = owner;
            // Catch up faster the further behind it is, up to three times walking speed.
            speed = s.speed() * Mth.clamp(1.0 + (gap - s.startDistance()) * 0.35, 1.0, 3.0);
        } else if (s.wanders() && state.idleTicks >= s.wanderAfterTicks()) {
            if (state.wanderTarget == null) {
                if (state.wanderPause > 0) {
                    state.wanderPause--;
                } else {
                    state.wanderTarget = pickWanderTarget(owner, s.wanderRadius(), level.getRandom());
                }
            }
            if (state.wanderTarget != null) {
                goal = state.wanderTarget;
                speed = s.speed() * 0.5;
            }
        } else {
            state.wanderTarget = null;
        }

        state.previousPosition = state.position;
        state.previousYaw = state.yaw;

        double x = state.position.x;
        double z = state.position.z;
        float targetYaw = yawTowards(dx, dz);

        if (goal != null) {
            double gx = goal.x - x;
            double gz = goal.z - z;
            double remaining = Math.sqrt(gx * gx + gz * gz);
            // Followers stop at stopDistance from the player; wandering walks right up to the spot.
            double stopAt = state.following ? s.stopDistance() * 0.8 : 0.1;
            if (remaining > stopAt) {
                double step = Math.min(speed, remaining - stopAt);
                double nx = x + gx / remaining * step;
                double nz = z + gz / remaining * step;
                double ground = groundAt(level, nx, nz, state.groundY);
                if (Double.isNaN(ground)) {
                    // Wall in the way.
                    state.stuckTicks++;
                    if (!state.following) {
                        state.wanderTarget = null;
                        state.wanderPause = pause(level.getRandom());
                    }
                } else {
                    x = nx;
                    z = nz;
                    state.stuckTicks = 0;
                }
                targetYaw = yawTowards(gx, gz);
            } else if (!state.following) {
                // Reached the wander spot: pause, then pick another.
                state.wanderTarget = null;
                state.wanderPause = pause(level.getRandom());
            }
        } else {
            state.stuckTicks = 0;
        }

        // Settle onto the ground under the new spot: step up gradually, fall gradually.
        double ground = groundAt(level, x, z, state.groundY);
        if (Double.isNaN(ground)) {
            // Something was placed on top of it: stay at this height rather than sink into the ground.
        } else if (ground == Double.NEGATIVE_INFINITY) {
            state.groundY -= FALL_SPEED;
        } else if (ground > state.groundY) {
            state.groundY = Math.min(ground, state.groundY + STEP_UP_SPEED);
        } else {
            state.groundY = Math.max(ground, state.groundY - FALL_SPEED);
        }

        state.position = new Vec3(x, state.groundY + centerLift, z);
        state.walking = state.position.subtract(state.previousPosition).horizontalDistanceSqr() > WALKING_SQR;
        state.yaw = Mth.rotLerp(state.walking ? 0.35f : 0.12f, state.yaw, targetYaw);
    }

    /**
     * Top of the ground a walking follower would stand on at (x, z), coming from height fromY.
     * NaN: blocked (the step up is higher than a block, or there's no headroom).
     * NEGATIVE_INFINITY: no ground within MAX_GROUND_SCAN below (a drop; it falls).
     */
    private static double groundAt(Level level, double x, double z, double fromY) {
        double limit = fromY + MAX_STEP_UP + 1.0E-4;
        int top = Mth.floor(limit);
        int bottom = Mth.floor(fromY - MAX_GROUND_SCAN);
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = BlockPos.containing(x, y, z);
            FluidState fluid = level.getFluidState(pos);
            BlockState block = level.getBlockState(pos);
            VoxelShape shape = block.getCollisionShape(level, pos);
            double surface = Double.NEGATIVE_INFINITY;
            if (!shape.isEmpty()) surface = y + shape.max(Direction.Axis.Y);
            if (!fluid.isEmpty()) surface = Math.max(surface, y + fluid.getHeight(level, pos));
            if (surface == Double.NEGATIVE_INFINITY) continue;
            if (surface > limit) {
                // Part of a wall or ceiling above the reachable height: keep looking below it.
                continue;
            }
            return hasHeadroom(level, x, surface, z) ? surface : Double.NaN;
        }
        return Double.NEGATIVE_INFINITY;
    }

    private static boolean hasHeadroom(Level level, double x, double surface, double z) {
        BlockPos above = BlockPos.containing(x, surface + 1.0E-3, z);
        VoxelShape shape = level.getBlockState(above).getCollisionShape(level, above);
        return shape.isEmpty() || above.getY() + shape.min(Direction.Axis.Y) >= surface + HEADROOM;
    }

    /**
     * Puts a walking follower on the ground behind the player, or at their feet if there's no room there.
     * With no ground anywhere near them (gliding, falling, flying in creative) it waits where it is and
     * catches up once they land; a follower that hasn't been placed yet starts at their feet.
     */
    private static void teleportBehind(State state, AbstractClientPlayer player, Level level, double centerLift) {
        Vec3 spot = behind(player, state.settings);
        double ground = groundAt(level, spot.x, spot.z, player.getY());
        if (Double.isNaN(ground) || ground == Double.NEGATIVE_INFINITY) {
            spot = player.position();
            ground = groundAt(level, spot.x, spot.z, player.getY());
        }
        if (Double.isNaN(ground) || ground == Double.NEGATIVE_INFINITY) {
            if (state.position != null) {
                state.previousPosition = state.position;
                state.previousYaw = state.yaw;
                state.stuckTicks = 0;
                state.walking = false;
                return;
            }
            ground = player.getY();
        }
        state.groundY = ground;
        state.following = false;
        state.stuckTicks = 0;
        state.wanderTarget = null;
        state.wanderPause = 0;
        state.walking = false;
        place(state, new Vec3(spot.x, ground + centerLift, spot.z), player.yBodyRot);
    }

    private static Vec3 pickWanderTarget(Vec3 owner, float radius, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = radius * (0.4 + 0.6 * random.nextDouble());
        return new Vec3(owner.x + Math.cos(angle) * distance, owner.y, owner.z + Math.sin(angle) * distance);
    }

    private static int pause(RandomSource random) {
        return WANDER_PAUSE_MIN + random.nextInt(WANDER_PAUSE_MAX - WANDER_PAUSE_MIN + 1);
    }

    // ---------------------------------------------------------------- shared

    /** The spot behind the player's shoulder (at their feet height): distance behind, side to their right. */
    private static Vec3 behind(AbstractClientPlayer player, FollowerSettings s) {
        double radians = Math.toRadians(player.yBodyRot);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = -Math.cos(radians);
        double rightZ = -Math.sin(radians);
        return player.position().add(
                -forwardX * s.distance() + rightX * s.side(),
                0,
                -forwardZ * s.distance() + rightZ * s.side());
    }

    /** Minecraft yaw that faces along (dx, dz). */
    private static float yawTowards(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static void place(State state, Vec3 position, float yaw) {
        state.position = position;
        state.previousPosition = position;
        state.yaw = yaw;
        state.previousYaw = yaw;
    }

    /** @return where this player's follower is right now (for particles), or null if they have none. */
    public static Vec3 positionOf(UUID player) {
        State state = STATES.get(player);
        return state == null ? null : state.position;
    }

    /** @return true while this player's walking follower is moving (animateWhen "walking"). */
    public static boolean isWalking(UUID player) {
        State state = STATES.get(player);
        return state != null && state.walking;
    }

    static Iterable<State> states() {
        return STATES.values();
    }

    public static void clear() {
        STATES.clear();
        lastLevel = null;
    }
}
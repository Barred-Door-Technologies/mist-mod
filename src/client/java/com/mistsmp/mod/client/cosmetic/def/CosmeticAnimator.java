package com.mistsmp.mod.client.cosmetic.def;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Picks which flipbook frame a cosmetic shows right now.
 *
 * Frames advance on the entity's own tick counter, which runs every client tick for every visible
 * player. When the definition's animateWhen condition isn't met, the first frame is shown
 * (wings folded, for example), and the animation picks up again as soon as the condition is true.
 */
public final class CosmeticAnimator {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");

    /** Limb-swing speed above which a player counts as moving. */
    private static final float WALK_SPEED_THRESHOLD = 0.05f;
    /** Squared horizontal distance per tick above which a player counts as moving (about 0.03 blocks). */
    private static final double MOVED_DISTANCE_SQR = 0.001;

    /** Cosmetics we've already logged, so the log gets one line per cosmetic, not one per frame. */
    private static final Set<Identifier> LOGGED = ConcurrentHashMap.newKeySet();

    private CosmeticAnimator() {
    }

    public static Identifier currentFrame(LivingEntity entity, CosmeticDefinition definition) {
        if (!definition.isAnimated()) {
            return definition.frames().getFirst();
        }
        if (LOGGED.add(definition.id())) {
            LOGGER.info("Animating {}: {} frames, {} ticks each, when {}",
                    definition.id(), definition.frames().size(), definition.frameTicks(), definition.animateWhen());
        }
        if (!conditionMet(entity, definition.animateWhen())) {
            return definition.frames().getFirst();
        }
        int index = (entity.tickCount / definition.frameTicks()) % definition.frames().size();
        return definition.frames().get(index);
    }

    public static boolean conditionMet(LivingEntity entity, CosmeticDefinition.AnimateWhen when) {
        return switch (when) {
            case ALWAYS -> true;
            case MOVING -> isMoving(entity);
            case SPRINTING -> entity.isSprinting();
            case FLYING -> entity.isFallFlying();
            case IDLE -> !isMoving(entity);
        };
    }

    /** Moving if the walk animation is going or the player's position changed since last tick. */
    public static boolean isMoving(LivingEntity entity) {
        if (entity.isFallFlying() || entity.isSwimming()) return true;
        if (entity.walkAnimation.speed() > WALK_SPEED_THRESHOLD) return true;
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        return dx * dx + dz * dz > MOVED_DISTANCE_SQR;
    }
}
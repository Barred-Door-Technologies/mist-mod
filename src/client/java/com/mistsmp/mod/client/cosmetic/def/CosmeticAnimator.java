package com.mistsmp.mod.client.cosmetic.def;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Picks which flipbook frame a cosmetic shows right now.
 *
 * Frames advance on the world clock, so every player wearing the same cosmetic animates in step.
 * When the definition's animateWhen condition isn't met, the first frame is shown (wings folded,
 * for example), and the animation picks up again as soon as the condition is true.
 */
public final class CosmeticAnimator {

    /** Limb-swing speed above which a player counts as moving. Works for other players, not just you. */
    private static final float MOVING_THRESHOLD = 0.05f;

    private CosmeticAnimator() {
    }

    public static Identifier currentFrame(LivingEntity entity, CosmeticDefinition definition) {
        if (!definition.isAnimated() || !conditionMet(entity, definition.animateWhen())) {
            return definition.frames().getFirst();
        }
        long ticks = entity.level().getGameTime();
        int index = (int) ((ticks / definition.frameTicks()) % definition.frames().size());
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

    public static boolean isMoving(LivingEntity entity) {
        return entity.walkAnimation.speed() > MOVING_THRESHOLD || entity.isFallFlying() || entity.isSwimming();
    }
}
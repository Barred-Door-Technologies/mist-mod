package com.mistsmp.mod.client.cosmetic.def;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.world.entity.LivingEntity;

/**
 * The extra transform for one cosmetic on one frame: the definition's placement plus its spin and bob
 * at this moment. Computed while extracting the render state, applied while drawing.
 *
 * Applied in the model's display space (after the slot's vanilla positioning, before the model's own
 * Display settings), where +Y is up and 1 unit is 16 pixels. Spin turns around the vertical axis
 * through the model's anchor (for hats, the center of the head).
 */
public record CosmeticTransform(
        float offsetX, float offsetY, float offsetZ,
        float rotationX, float rotationY, float rotationZ,
        float scale, float spinDegrees
) {

    private static final float PIXEL = 1f / 16f;

    /** @return the transform for this frame, or null if the cosmetic has no placement or motion. */
    public static CosmeticTransform compute(CosmeticDefinition definition, LivingEntity entity, float partialTick) {
        CosmeticDefinition.Placement p = definition.placement();
        CosmeticDefinition.Motion m = definition.motion();
        if (p.isNone() && m.isNone()) {
            return null;
        }

        float ticks = entity.tickCount + partialTick;
        float spin = m.spinDegreesPerSecond() == 0f ? 0f : (m.spinDegreesPerSecond() * ticks / 20f) % 360f;
        float bob = m.bobHeightPixels() == 0f ? 0f
                : m.bobHeightPixels() * (float) Math.sin(2.0 * Math.PI * ticks / m.bobPeriodTicks());

        return new CosmeticTransform(p.offsetX(), p.offsetY() + bob, p.offsetZ(),
                p.rotationX(), p.rotationY(), p.rotationZ(), p.scale(), spin);
    }

    public void applyTo(PoseStack poseStack) {
        poseStack.translate(offsetX * PIXEL, offsetY * PIXEL, offsetZ * PIXEL);
        if (spinDegrees != 0f) poseStack.rotateDegrees(Axis.YP, spinDegrees);
        if (rotationX != 0f) poseStack.rotateDegrees(Axis.XP, rotationX);
        if (rotationY != 0f) poseStack.rotateDegrees(Axis.YP, rotationY);
        if (rotationZ != 0f) poseStack.rotateDegrees(Axis.ZP, rotationZ);
        if (scale != 1f) poseStack.scale(scale, scale, scale);
    }
}
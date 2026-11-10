package com.mistsmp.mod.client.cosmetic;

import org.joml.Quaternionf;

import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws BACK wings in place of the elytra, called from WingsLayerMixin.
 *
 * Each wing model is authored folded, hinge at pixel (8,8,8). The hinge sits on the back, 2 px below the
 * neck. The wing turns by how far the vanilla elytra wing has turned away from its standing pose, so
 * "folded" in Blockbench is folded in game, and gliding spreads the wings the way it spreads an elytra.
 */
public final class WingsRenderer {

    /** Vanilla elytra wing angles while standing (radians). */
    private static final float REST_X = (float) (Math.PI / 12);
    private static final float REST_Z = (float) (-Math.PI / 12);

    private WingsRenderer() {
    }

    /** @return true if wings were drawn, so vanilla's elytra should be skipped. */
    public static boolean submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, HumanoidRenderState state) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        ItemStackRenderState left = cosmeticState.mistsmp$getLeftWing();
        if (left.isEmpty()) {
            return false;
        }
        ItemStackRenderState right = cosmeticState.mistsmp$getRightWing();
        CosmeticTransform transform = cosmeticState.mistsmp$getWingTransform();
        float wingDrop = state.isCrouching ? 3.0F : 0.0F;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        submitWing(poseStack, collector, lightCoords, state, left, transform, wingDrop,
                state.elytraRotX, state.elytraRotY, state.elytraRotZ, REST_X, 0.0F, REST_Z);
        if (!right.isEmpty()) {
            submitWing(poseStack, collector, lightCoords, state, right, transform, wingDrop,
                    state.elytraRotX, -state.elytraRotY, -state.elytraRotZ, REST_X, 0.0F, -REST_Z);
        }
        poseStack.popPose();
        return true;
    }

    private static void submitWing(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, HumanoidRenderState state,
                                   ItemStackRenderState wing, CosmeticTransform transform, float wingDrop,
                                   float x, float y, float z, float restX, float restY, float restZ) {
        poseStack.pushPose();
        // Hinge: centered, on the back, 2 px below the neck (entity model space is Y down).
        poseStack.translate(0.0F, (2.0F + wingDrop) / 16.0F, 0.0F);
        Quaternionf rest = new Quaternionf().rotationZYX(restZ, restY, restX);
        poseStack.rotate(new Quaternionf().rotationZYX(z, y, x).mul(rest.conjugate()));
        // To Blockbench space (Y up, player facing +Z). Item models draw centered on pixel (8,8,8), the hinge.
        poseStack.scale(1.0F, -1.0F, -1.0F);
        if (transform != null) {
            transform.applyTo(poseStack);
        }
        wing.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws a cosmetic model attached to the body bone, so it follows crouching and body turns.
 * BodyCosmetics decides what (if anything) to draw while extracting the render state.
 *
 * Coordinates: model pixel (8, 12, 10) on back_reference.json, the top center of the body,
 * lands on the body pivot (the neck). Model +Y is up and model +Z (south) is the way the player faces.
 */
public final class BodyCosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public BodyCosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        ItemStackRenderState bodyItem = cosmeticState.mistsmp$getBodyItem();
        if (state.isInvisible || bodyItem.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        PlayerModel model = this.getParentModel();
        model.root().translateAndRotate(poseStack);
        model.body.translateAndRotate(poseStack);

        // Entity model space is Y down with the player facing -Z. Flip to Blockbench space (Y up, facing +Z).
        poseStack.scale(1.0F, -1.0F, -1.0F);
        // Item models are drawn centered on pixel (8, 8, 8). Shift so pixel (8, 12, 10) sits on the pivot.
        poseStack.translate(0.0F, -0.25F, -0.125F);

        CosmeticTransform transform = cosmeticState.mistsmp$getBodyTransform();
        if (transform != null) {
            transform.applyTo(poseStack);
        }

        bodyItem.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
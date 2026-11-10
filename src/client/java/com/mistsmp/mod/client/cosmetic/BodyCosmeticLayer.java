package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws cosmetic models attached to the player's bones, so they follow crouching, turning, and walking.
 * BodyCosmetics and FeetCosmetics decide what (if anything) to draw while extracting the render state.
 *
 * Body (CHEST models): back_reference pixel (8, 12, 10), the top center of the body, lands on the body pivot.
 * Legs (FEET models): feet_reference pixel (8, 12, 8), the top center of the leg, lands on the leg pivot.
 * Model +Y is up and model +Z (south) is the way the player faces.
 */
public final class BodyCosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public BodyCosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        PlayerModel model = this.getParentModel();

        submitOnPart(poseStack, submitNodeCollector, lightCoords, state, model, model.body,
                cosmeticState.mistsmp$getBodyItem(), cosmeticState.mistsmp$getBodyTransform(), -0.125F);
        CosmeticTransform bootTransform = cosmeticState.mistsmp$getBootTransform();
        submitOnPart(poseStack, submitNodeCollector, lightCoords, state, model, model.rightLeg,
                cosmeticState.mistsmp$getRightBoot(), bootTransform, 0.0F);
        submitOnPart(poseStack, submitNodeCollector, lightCoords, state, model, model.leftLeg,
                cosmeticState.mistsmp$getLeftBoot(), bootTransform, 0.0F);

        // FACE: same placement as a head item, so the Blockbench Display tab Head slot applies.
        ItemStackRenderState face = cosmeticState.mistsmp$getFaceItem();
        if (!face.isEmpty()) {
            poseStack.pushPose();
            model.root().translateAndRotate(poseStack);
            model.head.translateAndRotate(poseStack);
            CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
            CosmeticTransform faceTransform = cosmeticState.mistsmp$getFaceTransform();
            if (faceTransform != null) {
                faceTransform.applyTo(poseStack);
            }
            face.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }

    /**
     * @param zShift moves the anchor along Blockbench Z: -0.125 puts pixel z=10 (body center) on the pivot,
     *               0 puts pixel z=8 (leg center) on it.
     */
    private void submitOnPart(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state,
                              PlayerModel model, ModelPart part, ItemStackRenderState item, CosmeticTransform transform, float zShift) {
        if (item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        model.root().translateAndRotate(poseStack);
        part.translateAndRotate(poseStack);

        // Entity model space is Y down with the player facing -Z. Flip to Blockbench space (Y up, facing +Z).
        poseStack.scale(1.0F, -1.0F, -1.0F);
        // Item models are drawn centered on pixel (8, 8, 8). Shift so pixel (8, 12, z) sits on the pivot.
        poseStack.translate(0.0F, -0.25F, zShift);

        if (transform != null) {
            transform.applyTo(poseStack);
        }
        item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
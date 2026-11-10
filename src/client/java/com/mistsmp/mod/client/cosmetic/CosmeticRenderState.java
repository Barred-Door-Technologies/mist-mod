package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;

/**
 * Extra per-frame cosmetic data carried on vanilla's HumanoidRenderState (added by HumanoidRenderStateMixin).
 * Render states are reused between frames, so every field is reset on each extraction.
 */
public interface CosmeticRenderState {

    CosmeticTransform mistsmp$getHeadTransform();

    void mistsmp$setHeadTransform(CosmeticTransform transform);

    /** The model drawn on the body bone by BodyCosmeticLayer. Empty when there is nothing to draw. */
    ItemStackRenderState mistsmp$getBodyItem();

    CosmeticTransform mistsmp$getBodyTransform();

    void mistsmp$setBodyTransform(CosmeticTransform transform);

    /** BACK wings, drawn by WingsRenderer in place of the elytra. Empty when there are none. */
    ItemStackRenderState mistsmp$getLeftWing();

    ItemStackRenderState mistsmp$getRightWing();

    CosmeticTransform mistsmp$getWingTransform();

    void mistsmp$setWingTransform(CosmeticTransform transform);

    /** BACK cape texture path, or null. Applied to the player's skin in AvatarRendererMixin. */
    Identifier mistsmp$getCapeTexture();

    void mistsmp$setCapeTexture(Identifier texture);
}
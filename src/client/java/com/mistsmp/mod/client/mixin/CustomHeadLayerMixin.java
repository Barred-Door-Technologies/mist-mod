package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.cosmetic.CosmeticRenderState;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Applies a head cosmetic's placement, spin and bob. Runs right before vanilla draws the head item,
 * after it has moved to the head and inside vanilla's push/pop, so nothing leaks to other layers.
 */
@Mixin(CustomHeadLayer.class)
abstract class CustomHeadLayerMixin {

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void mistsmp$applyHeadTransform(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                            LivingEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (state instanceof CosmeticRenderState cosmetic) {
            CosmeticTransform transform = cosmetic.mistsmp$getHeadTransform();
            if (transform != null) {
                transform.applyTo(poseStack);
            }
        }
    }
}
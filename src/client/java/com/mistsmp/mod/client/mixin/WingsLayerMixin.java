package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.cosmetic.WingsRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Draws BACK wings instead of the elytra when a player has wings equipped and is wearing an elytra. */
@Mixin(WingsLayer.class)
abstract class WingsLayerMixin {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void mistsmp$drawCosmeticWings(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                           HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (WingsRenderer.submit(poseStack, collector, lightCoords, state)) {
            ci.cancel();
        }
    }
}
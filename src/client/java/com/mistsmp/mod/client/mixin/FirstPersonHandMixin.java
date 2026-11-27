package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mistsmp.mod.client.cosmetic.HandsCosmetics;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

/**
 * First person: vanilla draws your bare arm (empty hand, or holding a map) with renderRightHand and
 * renderLeftHand. After it does, draw your HANDS glove on that arm, in the same pose. Only the local
 * player's arms are ever drawn here, so HandsCosmetics reads the local player's loadout.
 */
@Mixin(AvatarRenderer.class)
abstract class FirstPersonHandMixin {

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void mistsmp$rightGlove(CallbackInfo ci,
                                    @Local(argsOnly = true) PoseStack poseStack,
                                    @Local(argsOnly = true) SubmitNodeCollector collector,
                                    @Local(argsOnly = true) int lightCoords) {
        PlayerModel model = ((AvatarRenderer<?>) (Object) this).getModel();
        HandsCosmetics.submitFirstPerson(model.rightArm, true, poseStack, collector, lightCoords);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void mistsmp$leftGlove(CallbackInfo ci,
                                   @Local(argsOnly = true) PoseStack poseStack,
                                   @Local(argsOnly = true) SubmitNodeCollector collector,
                                   @Local(argsOnly = true) int lightCoords) {
        PlayerModel model = ((AvatarRenderer<?>) (Object) this).getModel();
        HandsCosmetics.submitFirstPerson(model.leftArm, false, poseStack, collector, lightCoords);
    }
}
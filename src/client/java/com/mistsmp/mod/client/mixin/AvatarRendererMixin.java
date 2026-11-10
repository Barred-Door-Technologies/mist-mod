package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.cosmetic.CosmeticRenderState;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * skin and showCape are set at the very end of AvatarRenderer.extractRenderState, after the humanoid hook
 * has run, so cape changes happen here.
 *
 * - A BACK cape replaces the player's cape and always shows (vanilla also puts it on the elytra).
 * - Any cape is hidden while a CHEST model (backpack) shows, since it would clip through it.
 */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void mistsmp$applyCapeCosmetics(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;

        Identifier cape = cosmeticState.mistsmp$getCapeTexture();
        if (cape != null) {
            PlayerSkin skin = state.skin;
            ClientAsset.Texture texture = new ClientAsset.ResourceTexture(cape, cape);
            state.skin = new PlayerSkin(skin.body(), texture, null, skin.model(), skin.secure());
            state.showCape = true;
        }

        if (!cosmeticState.mistsmp$getBodyItem().isEmpty()) {
            state.showCape = false;
        }
    }
}
package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.cosmetic.HeadCosmetics;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;

/**
 * AvatarRenderer.extractRenderState calls this after LivingEntityRenderer has filled headItem,
 * and this method fills headEquipment. Nothing after it touches either field, so the end of
 * this method is the one place where both are final and safe to replace.
 */
@Mixin(HumanoidMobRenderer.class)
abstract class HumanoidMobRendererMixin {

    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void mistsmp$applyHeadCosmetic(LivingEntity entity, HumanoidRenderState state, float partialTicks,
                                                  ItemModelResolver itemModelResolver, CallbackInfo ci) {
        HeadCosmetics.apply(entity, state, itemModelResolver);
    }
}
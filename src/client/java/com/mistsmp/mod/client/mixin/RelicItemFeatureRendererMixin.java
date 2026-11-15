package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mistsmp.mod.client.relic.RelicGlint;

import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Draws a relic item's glint with the gold texture. */
@Mixin(ItemFeatureRenderer.class)
abstract class RelicItemFeatureRendererMixin {

    @ModifyExpressionValue(method = "prepareMainSubmit", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemGlintRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemGlintSpecialRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;")
    })
    private RenderType mistsmp$goldGlint(RenderType original, @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        return RelicGlint.isRelicTints(submit.tintLayers()) ? RelicGlint.goldItemGlint(original) : original;
    }
}
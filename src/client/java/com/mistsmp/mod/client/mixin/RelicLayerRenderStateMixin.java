package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mistsmp.mod.client.relic.RelicGlint;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Ordinary item layers are drawn after they're submitted, when the relic flag is long gone. A relic layer
 * is submitted with its own copy of the tint array so the draw (RelicItemFeatureRendererMixin) can tell it apart.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
abstract class RelicLayerRenderStateMixin {

    @ModifyVariable(method = "submit", at = @At("STORE"), ordinal = 0)
    private int[] mistsmp$markRelicTints(int[] tints) {
        return RelicGlint.isActive() ? RelicGlint.markRelicTints(tints) : tints;
    }
}
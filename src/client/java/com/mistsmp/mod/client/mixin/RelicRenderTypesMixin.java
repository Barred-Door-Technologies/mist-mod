package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mistsmp.mod.client.relic.RelicGlint;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Glints picked while a relic is being submitted: worn armor (and its trim), shields, and tridents in hand.
 * Outside a relic submit these return vanilla's purple ones as usual.
 */
@Mixin(RenderTypes.class)
abstract class RelicRenderTypesMixin {

    @Inject(method = "armorCutoutNoCullGlint", at = @At("HEAD"), cancellable = true)
    private static void mistsmp$goldArmorGlint(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (RelicGlint.isActive()) cir.setReturnValue(RelicGlint.ARMOR_CUTOUT_NO_CULL_GLINT.apply(texture));
    }

    @Inject(method = "entitySolidGlint", at = @At("HEAD"), cancellable = true)
    private static void mistsmp$goldEntityGlint(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (RelicGlint.isActive()) cir.setReturnValue(RelicGlint.ENTITY_SOLID_GLINT.apply(texture));
    }

    @Inject(method = "patternedShieldGlint", at = @At("HEAD"), cancellable = true)
    private static void mistsmp$goldShieldGlint(CallbackInfoReturnable<RenderType> cir) {
        if (RelicGlint.isActive()) cir.setReturnValue(RelicGlint.PATTERNED_SHIELD_GLINT);
    }

    @Inject(method = "trimmedArmorGlint", at = @At("HEAD"), cancellable = true)
    private static void mistsmp$goldTrimGlint(CallbackInfoReturnable<RenderType> cir) {
        if (RelicGlint.isActive()) cir.setReturnValue(RelicGlint.TRIMMED_ARMOR_GLINT);
    }
}
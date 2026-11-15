package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.relic.RelicGlint;
import com.mistsmp.mod.client.relic.RelicRenderState;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Remembers whether the item behind this render state is a relic, and flags it while it's submitted. */
@Mixin(ItemStackRenderState.class)
abstract class RelicRenderStateMixin implements RelicRenderState {

    @Unique
    private boolean mistsmp$relic;

    @Unique
    private boolean mistsmp$previousActive;

    @Override
    public boolean mistsmp$isRelic() {
        return mistsmp$relic;
    }

    @Override
    public void mistsmp$setRelic(boolean relic) {
        mistsmp$relic = relic;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void mistsmp$clearRelic(CallbackInfo ci) {
        mistsmp$relic = false;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void mistsmp$beginRelic(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline, CallbackInfo ci) {
        mistsmp$previousActive = RelicGlint.setActive(mistsmp$relic);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void mistsmp$endRelic(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline, CallbackInfo ci) {
        RelicGlint.setActive(mistsmp$previousActive);
    }
}
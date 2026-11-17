package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mistsmp.mod.client.title.TitleEffects;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;

/**
 * Animated titles carry their settings in the text's insertion field, which vanilla pastes into the
 * chat box on shift-click. Shift-clicking a title does nothing instead.
 */
@Mixin(ChatScreen.class)
abstract class ChatScreenInsertionMixin {

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void mistsmp$skipTitleMarker(Style clicked, boolean allowInsertions, CallbackInfoReturnable<Boolean> cir) {
        if (allowInsertions && TitleEffects.isMarker(clicked.getInsertion())) {
            cir.setReturnValue(false);
        }
    }
}
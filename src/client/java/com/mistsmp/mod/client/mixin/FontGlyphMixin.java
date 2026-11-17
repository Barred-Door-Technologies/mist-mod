package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mistsmp.mod.client.title.TitleEffects;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;

/**
 * Animated titles. Every letter of text drawn on screen (chat, chat history, the tab list) passes
 * through Font.PreparedTextBuilder.accept(position, style, codepoint), and text is prepared again
 * every frame, so recoloring here animates it.
 *
 * Only letters whose style carries a "mistfx:" marker are touched; everything else runs vanilla's
 * code unchanged. For a marked letter this picks the glyph for the new style (glitch scrambles it),
 * nudges the pen by the motion offset, draws through vanilla's own accept, and puts the pen back,
 * so the next letter lands exactly where vanilla would put it.
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
abstract class FontGlyphMixin {

    @Shadow
    @Final
    Font this$0;

    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    public abstract boolean accept(int position, Style style, BakedGlyph glyph);

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"), cancellable = true)
    private void mistsmp$animateTitle(int position, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
        TitleEffects.Glyph animated = TitleEffects.apply(style);
        if (animated == null) return;

        BakedGlyph glyph = ((FontAccessor) this$0).mistsmp$getGlyph(codepoint, animated.style());
        this.x += animated.dx();
        this.y += animated.dy();
        boolean result = accept(position, animated.style(), glyph);
        this.x -= animated.dx();
        this.y -= animated.dy();
        cir.setReturnValue(result);
    }
}
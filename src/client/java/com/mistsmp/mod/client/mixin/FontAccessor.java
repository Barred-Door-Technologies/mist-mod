package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;

/** Font.getGlyph is private; FontGlyphMixin needs it to pick the glyph for an animated title's new style. */
@Mixin(Font.class)
public interface FontAccessor {

    @Invoker("getGlyph")
    BakedGlyph mistsmp$getGlyph(int codepoint, Style style);
}
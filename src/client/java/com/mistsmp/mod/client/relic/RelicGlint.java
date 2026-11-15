package com.mistsmp.mod.client.relic;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Relics shimmer gold instead of purple. The glint is the same shader as vanilla's; only its texture changes.
 *
 * There are two ways a glint gets drawn, so there are two hooks:
 * 1. Things drawn as they are submitted (worn armor, shields, tridents in hand). While a relic is being
 *    submitted, active is true and RelicRenderTypesMixin hands out the gold versions of those render types.
 * 2. Ordinary items (inventory, hand, dropped, item frames) are drawn later, after submission. Each relic
 *    layer is submitted with its own tint array, remembered here, and RelicItemFeatureRendererMixin swaps in
 *    the gold glint when it draws a submit with one of those arrays.
 *
 * Everything here runs on the render thread.
 */
public final class RelicGlint {

    public static final Identifier GOLD_GLINT_ITEM = Identifier.fromNamespaceAndPath("mistsmp", "textures/misc/relic_glint_item.png");
    public static final Identifier GOLD_GLINT_ARMOR = Identifier.fromNamespaceAndPath("mistsmp", "textures/misc/relic_glint_armor.png");

    /** True while a relic is being submitted for rendering. */
    private static boolean active;

    /** Tint arrays of relic item layers waiting to be drawn. Arrays compare by identity; weak so drawn ones go away. */
    private static final Set<int[]> RELIC_TINTS = Collections.newSetFromMap(new WeakHashMap<>());

    private RelicGlint() {
    }

    public static boolean isActive() {
        return active;
    }

    /** Sets the flag and returns what it was, so callers can restore it (relics can nest, e.g. a relic in an item frame). */
    public static boolean setActive(boolean value) {
        boolean previous = active;
        active = value;
        return previous;
    }

    /** A fresh tint array for a relic layer, remembered so the later draw can recognise it. */
    public static int[] markRelicTints(int[] tints) {
        int[] own = tints.clone();
        RELIC_TINTS.add(own);
        return own;
    }

    public static boolean isRelicTints(int[] tints) {
        return tints != null && RELIC_TINTS.contains(tints);
    }

    // =========================================================================
    // Item glints (path 2)
    // =========================================================================

    private static final Map<RenderType, RenderType> ITEM_GLINTS = new IdentityHashMap<>();

    /** The gold version of an item glint render type, or the original if it isn't one we know. */
    public static RenderType goldItemGlint(RenderType vanilla) {
        if (ITEM_GLINTS.isEmpty()) buildItemGlints();
        return ITEM_GLINTS.getOrDefault(vanilla, vanilla);
    }

    private static void buildItemGlints() {
        for (Identifier atlas : new Identifier[]{TextureAtlas.LOCATION_ITEMS, TextureAtlas.LOCATION_BLOCKS}) {
            boolean items = atlas.equals(TextureAtlas.LOCATION_ITEMS);
            ITEM_GLINTS.put(items ? Sheets.cutoutItemGlintSheet() : Sheets.cutoutBlockItemGlintSheet(),
                    itemGlint("mistsmp_item_cutout_glint", atlas, false, false));
            ITEM_GLINTS.put(items ? Sheets.cutoutItemGlintSpecialSheet() : Sheets.cutoutBlockItemGlintSpecialSheet(),
                    itemGlint("mistsmp_item_cutout_glint_special", atlas, false, true));
            ITEM_GLINTS.put(items ? Sheets.translucentItemGlintSheet() : Sheets.translucentBlockItemGlintSheet(),
                    itemGlint("mistsmp_item_translucent_glint", atlas, true, false));
            ITEM_GLINTS.put(items ? Sheets.translucentItemGlintSpecialSheet() : Sheets.translucentBlockItemGlintSpecialSheet(),
                    itemGlint("mistsmp_item_translucent_glint_special", atlas, true, true));
        }
    }

    /** Mirrors RenderTypes.ITEM_CUTOUT_GLINT and its three siblings, with the gold texture. */
    private static RenderType itemGlint(String name, Identifier atlas, boolean translucent, boolean special) {
        RenderSetup.RenderSetupBuilder builder;
        if (translucent) {
            builder = RenderSetup.builder(special ? RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL : RenderPipelines.ITEM_TRANSLUCENT_GLINT)
                    .setOitPipelines(special ? RenderPipelines.OIT_ITEM_GLINT_SPECIAL : RenderPipelines.OIT_ITEM_GLINT);
        } else {
            builder = RenderSetup.builder(special ? RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL : RenderPipelines.ITEM_CUTOUT_GLINT);
        }
        builder.withTexture("Sampler0", atlas)
                .withTexture("GlintSampler", GOLD_GLINT_ITEM)
                .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                .useLightmap()
                .useOverlay()
                .affectsCrumbling();
        if (translucent) builder.sortOnUpload();
        builder.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        return RenderType.create(name, builder.createRenderSetup());
    }

    // =========================================================================
    // Submit-time glints (path 1)
    // =========================================================================

    /** Mirrors RenderTypes.ARMOR_CUTOUT_NO_CULL_GLINT. */
    public static final Function<Identifier, RenderType> ARMOR_CUTOUT_NO_CULL_GLINT = Util.memoize(texture ->
            RenderType.create("mistsmp_armor_cutout_no_cull_glint", RenderSetup.builder(RenderPipelines.ARMOR_CUTOUT_NO_CULL_GLINT)
                    .withTexture("Sampler0", texture)
                    .withTexture("GlintSampler", GOLD_GLINT_ARMOR)
                    .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                    .useLightmap()
                    .useOverlay()
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));

    /** Mirrors RenderTypes.ENTITY_SOLID_GLINT (shields and tridents in hand). */
    public static final Function<Identifier, RenderType> ENTITY_SOLID_GLINT = Util.memoize(texture ->
            RenderType.create("mistsmp_entity_solid_glint", RenderSetup.builder(RenderPipelines.ENTITY_SOLID_GLINT)
                    .withTexture("Sampler0", texture)
                    .withTexture("GlintSampler", GOLD_GLINT_ITEM)
                    .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                    .createRenderSetup()));

    /** Mirrors RenderTypes.PATTERNED_SHIELD_GLINT (a shield with a banner on it). */
    public static final RenderType PATTERNED_SHIELD_GLINT = RenderType.create("mistsmp_patterned_shield_glint",
            RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", GOLD_GLINT_ITEM)
                    .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                    .withForcedSolidModelPhase()
                    .createRenderSetup());

    /** Mirrors RenderTypes.TRIMMED_ARMOR_GLINT (armor with a trim). */
    public static final RenderType TRIMMED_ARMOR_GLINT = RenderType.create("mistsmp_trimmed_armor_glint",
            RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", GOLD_GLINT_ARMOR)
                    .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .withForcedSolidModelPhase()
                    .createRenderSetup());
}
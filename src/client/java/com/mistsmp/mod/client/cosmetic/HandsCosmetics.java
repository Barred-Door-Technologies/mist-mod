package com.mistsmp.mod.client.cosmetic;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * HANDS (gloves): always shows (there is no vanilla glove item). Drawn on the arm bones, so it follows arm
 * swing, attacks, and crouching: by BodyCosmeticLayer in third person, and by FirstPersonHandMixin on your
 * own arms in first person.
 *
 * Two models: items/cosmetic/hands/<key>.json is the RIGHT hand and <key>_left.json is the left hand
 * (mirror the right one on X in Blockbench). Built on feet_reference (the arm is the same size as a leg):
 * arm [6,0,6]-[10,12,10], hand at the bottom (y=0), shoulder at y=12, front toward +Z, -X is the outside.
 * Flipbook frames need a _left model too.
 *
 * Anchoring: vanilla's arm pivot sits 2 px below the top of the arm and 1 px toward the body from its
 * middle (half a pixel on slim arms, which are 3 px wide), so model pixel (9, 10, 8) lands on the right
 * arm's pivot and (7, 10, 8) on the left (8.5 and 7.5 for slim arms).
 */
public final class HandsCosmetics {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    /** First person draws one hand at a time on the render thread; reused like the render state's fields. */
    private static final ItemStackRenderState FIRST_PERSON = new ItemStackRenderState();

    private HandsCosmetics() {
    }

    /** Third person: fills the render state's glove fields, or leaves them empty. */
    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        cosmeticState.mistsmp$getRightGlove().clear();
        cosmeticState.mistsmp$getLeftGlove().clear();
        cosmeticState.mistsmp$setGloveTransform(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.HANDS);
        if (cosmetic == null || !CosmeticModels.has(cosmetic)) {
            return;
        }

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        Identifier right = HeadCosmetics.frameFor(entity, cosmetic, definition);
        itemModelResolver.updateForLiving(cosmeticState.mistsmp$getRightGlove(), HeadCosmetics.stackFor(right), ItemDisplayContext.NONE, entity);

        Identifier left = leftModel(cosmetic, right);
        if (left != null) {
            itemModelResolver.updateForLiving(cosmeticState.mistsmp$getLeftGlove(), HeadCosmetics.stackFor(left), ItemDisplayContext.NONE, entity);
        }
        cosmeticState.mistsmp$setGloveTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }

    /**
     * First person: draws your own glove on the arm vanilla just drew. poseStack is the one vanilla drew the
     * arm with, before the arm's own transform (it is applied here, as vanilla does).
     */
    public static void submitFirstPerson(ModelPart arm, boolean rightArm, PoseStack poseStack,
                                         SubmitNodeCollector collector, int lightCoords) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null || !ClientSession.isWelcomed()) {
            return;
        }
        Identifier cosmetic = LoadoutCache.get(player.getUUID(), CosmeticSlot.HANDS);
        if (cosmetic == null || !CosmeticModels.has(cosmetic)) {
            return;
        }

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        Identifier model = HeadCosmetics.frameFor(player, cosmetic, definition);
        if (!rightArm) {
            model = leftModel(cosmetic, model);
            if (model == null) return;
        }
        minecraft.getItemModelResolver().updateForLiving(FIRST_PERSON, HeadCosmetics.stackFor(model), ItemDisplayContext.NONE, player);
        if (FIRST_PERSON.isEmpty()) {
            return;
        }

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        boolean slim = player.getSkin().model() == PlayerModelType.SLIM;

        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        toArmAnchor(poseStack, rightArm, slim);
        CosmeticTransform transform = CosmeticTransform.compute(definition, player, partialTick);
        if (transform != null) {
            transform.applyTo(poseStack);
        }
        FIRST_PERSON.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /**
     * From an arm part's own space (after translateAndRotate) to the glove model's space, so the model's arm
     * pivot pixel lands on the part's pivot. Model +Y is up and +Z is the way the player faces.
     */
    static void toArmAnchor(PoseStack poseStack, boolean rightArm, boolean slim) {
        // Entity model space is Y down with the player facing -Z. Flip to Blockbench space (Y up, facing +Z).
        poseStack.scale(1.0F, -1.0F, -1.0F);
        float inward = slim ? 0.5F : 1.0F;
        float pivotX = rightArm ? 8.0F + inward : 8.0F - inward;
        // Item models are drawn centered on pixel (8, 8, 8). Shift so pixel (pivotX, 10, 8) sits on the pivot.
        poseStack.translate((8.0F - pivotX) / 16.0F, -2.0F / 16.0F, 0.0F);
    }

    /** The left-hand model for this frame: <frame>_left, then <key>_left; null (and one warning) if neither exists. */
    private static Identifier leftModel(Identifier cosmetic, Identifier rightFrame) {
        Identifier left = rightFrame.withSuffix("_left");
        if (!CosmeticModels.has(left)) {
            left = cosmetic.withSuffix("_left");
        }
        if (CosmeticModels.has(left)) {
            return left;
        }
        if (WARNED.add(cosmetic)) {
            LOGGER.warn("{}: no left glove model {}; only the right glove will show", cosmetic, left);
        }
        return null;
    }
}
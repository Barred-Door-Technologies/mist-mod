package com.mistsmp.mod.client.cosmetic;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * BACK slot. The definition's "back": { "style": ... } picks one of two styles:
 *
 * "wings" (default): shows only while a glider (elytra) is worn, and replaces the elytra.
 *   Two models: items/cosmetic/back/<key>.json is the LEFT wing and <key>_right.json is the right wing
 *   (mirror the left one in Blockbench). Built on wing_reference: hinge at (8,8,8), folded down the back.
 *   The wings follow the elytra's fold and spread. Flipbook frames need a _right model too.
 *
 * "cape": a 64x32 texture in the vanilla cape layout at textures/entity/cosmetic/back/<key>.png.
 *   Always shows, like a vanilla cape, and moves like one. With an elytra on, the elytra uses it as its
 *   texture, like vanilla. Hidden while a CHEST model (backpack) shows, since it would clip.
 */
public final class BackCosmetics {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    public enum Style {
        WINGS, CAPE;

        static Style of(CosmeticDefinition definition) {
            JsonObject raw = definition.raw();
            if (raw.has("back") && raw.get("back").isJsonObject()) {
                JsonObject back = raw.getAsJsonObject("back");
                if (back.has("style") && back.get("style").isJsonPrimitive()) {
                    try {
                        return valueOf(back.get("style").getAsString().trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        // fall through to the default
                    }
                }
            }
            return WINGS;
        }
    }

    private BackCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        cosmeticState.mistsmp$getLeftWing().clear();
        cosmeticState.mistsmp$getRightWing().clear();
        cosmeticState.mistsmp$setWingTransform(null);
        cosmeticState.mistsmp$setCapeTexture(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.BACK);
        if (cosmetic == null) {
            return;
        }

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        if (Style.of(definition) == Style.CAPE) {
            Identifier texture = CosmeticTextures.entityTexture(cosmetic);
            if (CosmeticTextures.exists(texture)) {
                cosmeticState.mistsmp$setCapeTexture(texture);
            } else {
                warnOnce(cosmetic, "No cape texture at " + texture);
            }
            return;
        }

        ItemStack worn = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!worn.has(DataComponents.GLIDER) || !CosmeticModels.exists(cosmetic)) {
            return;
        }

        Identifier left = HeadCosmetics.frameFor(entity, cosmetic, definition);
        itemModelResolver.updateForLiving(cosmeticState.mistsmp$getLeftWing(), HeadCosmetics.stackFor(left), ItemDisplayContext.NONE, entity);

        Identifier right = left.withSuffix("_right");
        if (!CosmeticModels.exists(right)) {
            right = cosmetic.withSuffix("_right");
        }
        if (CosmeticModels.exists(right)) {
            itemModelResolver.updateForLiving(cosmeticState.mistsmp$getRightWing(), HeadCosmetics.stackFor(right), ItemDisplayContext.NONE, entity);
        } else {
            warnOnce(cosmetic, "No right wing model " + right + "; only the left wing will show");
        }
        cosmeticState.mistsmp$setWingTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }

    private static void warnOnce(Identifier cosmetic, String message) {
        if (WARNED.add(cosmetic)) {
            LOGGER.warn("{}: {}", cosmetic, message);
        }
    }
}
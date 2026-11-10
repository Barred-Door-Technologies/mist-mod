package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * CHEST slot rule (3D model style): if a player has a CHEST cosmetic AND is wearing a chestplate,
 * the chestplate is hidden and the cosmetic model is drawn on the body bone by BodyCosmeticLayer.
 * Nothing worn means no cosmetic.
 *
 * An elytra also sits in the chest slot, but it belongs to the BACK slot, so a worn glider is
 * skipped here and left for BACK to handle.
 *
 * Models are authored on back_reference.json: the body is [4,0,8]-[12,12,12] and the player
 * faces +Z (south). Backpacks and capes go on the north side (z below 8).
 */
public final class BodyCosmetics {

    private BodyCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        // Render states are reused between frames, so clear last frame's model before anything else.
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        ItemStackRenderState bodyItem = cosmeticState.mistsmp$getBodyItem();
        bodyItem.clear();
        cosmeticState.mistsmp$setBodyTransform(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }

        ItemStack worn = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (worn.isEmpty() || worn.has(DataComponents.GLIDER)) {
            return;
        }

        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.CHEST);
        if (cosmetic == null || !CosmeticModels.has(cosmetic)) {
            return;
        }

        // Hide the real chestplate (body and arms).
        state.chestEquipment = ItemStack.EMPTY;

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        Identifier frame = HeadCosmetics.frameFor(entity, cosmetic, definition);
        itemModelResolver.updateForLiving(bodyItem, HeadCosmetics.stackFor(frame), ItemDisplayContext.NONE, entity);
        cosmeticState.mistsmp$setBodyTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }
}
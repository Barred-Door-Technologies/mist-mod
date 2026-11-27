package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Whether a slot's cosmetic is currently showing on a player, for things that happen outside the
 * renderers (cosmetic particles). Each rule mirrors the renderer named beside it; change both together.
 *
 * Model files are not required here: a cosmetic can be particles only, and those still follow the
 * slot's rule (a particle-only HEAD cosmetic shows while a helmet is worn).
 */
public final class CosmeticVisibility {

    private CosmeticVisibility() {
    }

    public static boolean isShowing(Player player, CosmeticSlot slot, CosmeticDefinition definition) {
        return switch (slot) {
            // HeadCosmetics: something is worn on the head
            case HEAD -> !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            // BodyCosmetics, ArmorCosmetics: a chestplate is worn, and an elytra doesn't count
            case CHEST -> isChestplate(player.getItemBySlot(EquipmentSlot.CHEST));
            // ArmorCosmetics: leggings are worn
            case LEGS -> !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
            // FeetCosmetics, ArmorCosmetics: boots are worn
            case FEET -> !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            // BackCosmetics: wings need a glider; a cape always shows (AvatarRendererMixin hides it under a CHEST model)
            case BACK -> BackCosmetics.Style.of(definition) == BackCosmetics.Style.CAPE
                    ? !chestModelShowing(player)
                    : player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER);
            // HeldCosmetics: a held item matches held.appliesTo
            case HELD -> heldSide(player, definition) != 0;
            // No vanilla item: always
            case FACE, FOLLOWER, AURA, HANDS -> true;
        };
    }

    /**
     * The side of the hand holding an item the HELD cosmetic applies to: 1 for the player's right,
     * -1 for the left, 0 for neither. The main hand wins when both match.
     */
    public static double heldSide(Player player, CosmeticDefinition definition) {
        double mainSide = mainHandSide(player);
        if (HeldCosmetics.appliesTo(definition, player.getMainHandItem())) return mainSide;
        if (HeldCosmetics.appliesTo(definition, player.getOffhandItem())) return -mainSide;
        return 0;
    }

    /** 1 when the main hand is the right hand, -1 for left-handed players. */
    public static double mainHandSide(Player player) {
        return player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
    }

    private static boolean isChestplate(ItemStack worn) {
        return !worn.isEmpty() && !worn.has(DataComponents.GLIDER);
    }

    /** BodyCosmetics: a CHEST model (backpack) shows while a chestplate is worn. */
    private static boolean chestModelShowing(Player player) {
        if (!isChestplate(player.getItemBySlot(EquipmentSlot.CHEST))) return false;
        Identifier chest = LoadoutCache.get(player.getUUID(), CosmeticSlot.CHEST);
        return chest != null && CosmeticModels.has(chest);
    }
}
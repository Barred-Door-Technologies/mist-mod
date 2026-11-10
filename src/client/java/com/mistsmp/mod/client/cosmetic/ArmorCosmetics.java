package com.mistsmp.mod.client.cosmetic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mistsmp.mod.client.cosmetic.def.CosmeticAnimator;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Texture style for CHEST, LEGS, and FEET: the cosmetic is a flat armor texture drawn by vanilla's armor
 * renderer in place of the worn piece. Same rule as every slot: it shows only while that piece is worn.
 *
 * A cosmetic uses this style when it has an equipment asset file:
 *   assets/<ns>/equipment/cosmetic/<slot>/<key>.json
 *     { "layers": { "humanoid": [ { "texture": "<ns>:cosmetic/<slot>/<key>" } ] } }
 *     (LEGS uses "humanoid_leggings" instead of "humanoid")
 *   texture: assets/<ns>/textures/entity/equipment/<layer>/cosmetic/<slot>/<key>.png (64x32, vanilla armor layout)
 *
 * CHEST and FEET can also be 3D models (BodyCosmetics, FeetCosmetics). If a cosmetic has a model, the
 * model wins. Flipbook frames work here too: each frame is another equipment asset id.
 * The worn piece's trim and enchantment glint are not drawn on the cosmetic.
 */
public final class ArmorCosmetics {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Map<String, ItemStack> STACKS = new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    private ArmorCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state) {
        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }
        ItemStack chest = replacement(entity, EquipmentSlot.CHEST, CosmeticSlot.CHEST);
        if (chest != null) state.chestEquipment = chest;
        ItemStack legs = replacement(entity, EquipmentSlot.LEGS, CosmeticSlot.LEGS);
        if (legs != null) state.legsEquipment = legs;
        ItemStack feet = replacement(entity, EquipmentSlot.FEET, CosmeticSlot.FEET);
        if (feet != null) state.feetEquipment = feet;
    }

    /** @return the stack to draw instead of the worn piece, or null to leave the worn piece alone. */
    private static ItemStack replacement(LivingEntity entity, EquipmentSlot equipmentSlot, CosmeticSlot slot) {
        ItemStack worn = entity.getItemBySlot(equipmentSlot);
        if (worn.isEmpty() || (equipmentSlot == EquipmentSlot.CHEST && worn.has(DataComponents.GLIDER))) {
            return null;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), slot);
        if (cosmetic == null || CosmeticModels.has(cosmetic)) {
            return null;
        }
        if (!assetExists(cosmetic)) {
            if (WARNED.add(cosmetic)) {
                LOGGER.warn("{}: no model and no equipment asset at {}", cosmetic, assetFile(cosmetic));
            }
            return null;
        }
        Identifier frame = CosmeticAnimator.currentFrame(entity, CosmeticDefinitions.get(cosmetic));
        if (!frame.equals(cosmetic) && !assetExists(frame)) {
            frame = cosmetic;
        }
        return stackFor(frame, equipmentSlot);
    }

    private static Identifier assetFile(Identifier asset) {
        return asset.withPath(path -> "equipment/" + path + ".json");
    }

    private static boolean assetExists(Identifier asset) {
        return CosmeticTextures.exists(assetFile(asset));
    }

    private static ItemStack stackFor(Identifier asset, EquipmentSlot equipmentSlot) {
        return STACKS.computeIfAbsent(asset + "|" + equipmentSlot.name(), key -> {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.EQUIPPABLE, Equippable.builder(equipmentSlot)
                    .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, asset))
                    .build());
            return stack;
        });
    }
}
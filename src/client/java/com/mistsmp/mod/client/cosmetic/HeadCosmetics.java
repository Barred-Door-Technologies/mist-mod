package com.mistsmp.mod.client.cosmetic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mistsmp.mod.client.cosmetic.def.CosmeticAnimator;
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
import net.minecraft.world.item.Items;

/**
 * HEAD slot rule: if a player has a HEAD cosmetic AND something is worn in their head slot,
 * the worn item is hidden and the cosmetic is drawn on the head bone instead.
 * Nothing worn means no cosmetic.
 *
 * Works by editing the render state after vanilla extracts it. Vanilla's CustomHeadLayer then
 * draws the cosmetic exactly like a carved pumpkin, using the model's "head" display transform.
 * The definition's placement, spin and bob ride along on the render state and are applied by
 * CustomHeadLayerMixin just before the model is drawn.
 */
public final class HeadCosmetics {

    /** One display-only stack per cosmetic id; the base item is irrelevant because item_model overrides it. */
    private static final Map<Identifier, ItemStack> STACKS = new ConcurrentHashMap<>();

    private HeadCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        // Render states are reused between frames, so clear last frame's transform before anything else.
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        cosmeticState.mistsmp$setHeadTransform(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }

        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.HEAD);
        if (cosmetic == null || entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }

        // No model in the loaded pack: leave the real helmet alone rather than draw the missing-model cube.
        if (!CosmeticModels.exists(cosmetic)) {
            return;
        }

        // Hide whatever is actually worn: armor helmets, skulls, and other head items.
        state.headEquipment = ItemStack.EMPTY;
        state.wornHeadType = null;
        state.wornHeadProfile = null;

        // Draw the cosmetic through the head item path instead, on the current flipbook frame.
        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        itemModelResolver.updateForLiving(state.headItem, stackFor(frameFor(entity, cosmetic, definition)), ItemDisplayContext.HEAD, entity);
        cosmeticState.mistsmp$setHeadTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }

    /** The flipbook frame to show now. A frame missing from the pack falls back to the base model. */
    static Identifier frameFor(LivingEntity entity, Identifier cosmetic, CosmeticDefinition definition) {
        Identifier frame = CosmeticAnimator.currentFrame(entity, definition);
        return frame.equals(cosmetic) || CosmeticModels.exists(frame) ? frame : cosmetic;
    }

    static ItemStack stackFor(Identifier cosmetic) {
        return STACKS.computeIfAbsent(cosmetic, id -> {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.ITEM_MODEL, id);
            return stack;
        });
    }
}
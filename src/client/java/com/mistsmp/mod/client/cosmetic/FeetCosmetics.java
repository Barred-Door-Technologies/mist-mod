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

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * FEET model style (3D boots): shows while boots are worn and hides them. Drawn on the leg bones by
 * BodyCosmeticLayer, so the boots follow walking.
 *
 * Two models: items/cosmetic/feet/<key>.json is the RIGHT boot and <key>_left.json is the left boot
 * (mirror the right one on X in Blockbench). Built on feet_reference: right leg [6,0,6]-[10,12,10],
 * sole at y=0, toe toward +Z, -X is the outside. Flipbook frames need a _left model too.
 * A FEET cosmetic with no model uses the texture style instead (ArmorCosmetics).
 */
public final class FeetCosmetics {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    private FeetCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        cosmeticState.mistsmp$getRightBoot().clear();
        cosmeticState.mistsmp$getLeftBoot().clear();
        cosmeticState.mistsmp$setBootTransform(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }
        ItemStack worn = entity.getItemBySlot(EquipmentSlot.FEET);
        if (worn.isEmpty()) {
            return;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.FEET);
        if (cosmetic == null || !CosmeticModels.has(cosmetic)) {
            return;
        }

        state.feetEquipment = ItemStack.EMPTY;

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        Identifier right = HeadCosmetics.frameFor(entity, cosmetic, definition);
        itemModelResolver.updateForLiving(cosmeticState.mistsmp$getRightBoot(), HeadCosmetics.stackFor(right), ItemDisplayContext.NONE, entity);

        Identifier left = right.withSuffix("_left");
        if (!CosmeticModels.has(left)) {
            left = cosmetic.withSuffix("_left");
        }
        if (CosmeticModels.has(left)) {
            itemModelResolver.updateForLiving(cosmeticState.mistsmp$getLeftBoot(), HeadCosmetics.stackFor(left), ItemDisplayContext.NONE, entity);
        } else if (WARNED.add(cosmetic)) {
            LOGGER.warn("{}: no left boot model {}; only the right boot will show", cosmetic, left);
        }
        cosmeticState.mistsmp$setBootTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }
}
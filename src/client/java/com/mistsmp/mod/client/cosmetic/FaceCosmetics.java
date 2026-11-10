package com.mistsmp.mod.client.cosmetic;

import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * FACE slot (masks, glasses): always shows, since there is no vanilla face item, and draws in addition
 * to whatever is on the head (a helmet or a HEAD cosmetic). Drawn on the head bone by BodyCosmeticLayer.
 *
 * Authored like hats: placement comes from the Blockbench Display tab, Head slot. In that view the
 * player's face is the model's NORTH side (the same as vanilla head items like the carved pumpkin).
 */
public final class FaceCosmetics {

    private FaceCosmetics() {
    }

    public static void apply(LivingEntity entity, HumanoidRenderState state, float partialTick, ItemModelResolver itemModelResolver) {
        CosmeticRenderState cosmeticState = (CosmeticRenderState) state;
        cosmeticState.mistsmp$getFaceItem().clear();
        cosmeticState.mistsmp$setFaceTransform(null);

        if (!(entity instanceof Player) || !ClientSession.isWelcomed()) {
            return;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.FACE);
        if (cosmetic == null || !CosmeticModels.exists(cosmetic)) {
            return;
        }

        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        Identifier frame = HeadCosmetics.frameFor(entity, cosmetic, definition);
        itemModelResolver.updateForLiving(cosmeticState.mistsmp$getFaceItem(), HeadCosmetics.stackFor(frame), ItemDisplayContext.HEAD, entity);
        cosmeticState.mistsmp$setFaceTransform(CosmeticTransform.compute(definition, entity, partialTick));
    }
}
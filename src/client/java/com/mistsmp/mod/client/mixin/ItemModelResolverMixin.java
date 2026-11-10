package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mistsmp.mod.client.cosmetic.HeldCosmetics;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Every held item (first person, and third person through updateForLiving) passes through
 * updateForTopItem. HELD cosmetics swap the stack here, before its model is looked up.
 */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {

    @ModifyVariable(
            method = "updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V",
            at = @At("HEAD"), argsOnly = true)
    private ItemStack mistsmp$swapHeldCosmetic(ItemStack stack,
                                               @Local(argsOnly = true) ItemDisplayContext context,
                                               @Local(argsOnly = true) ItemOwner owner) {
        return HeldCosmetics.replace(stack, context, owner);
    }
}
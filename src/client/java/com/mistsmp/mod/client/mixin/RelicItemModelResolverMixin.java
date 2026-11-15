package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.relic.RelicItems;
import com.mistsmp.mod.client.relic.RelicRenderState;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Every item render state is filled through appendItemLayers; mark the state when the item is a relic. */
@Mixin(ItemModelResolver.class)
abstract class RelicItemModelResolverMixin {

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void mistsmp$markRelic(ItemStackRenderState output, ItemStack stack, ItemDisplayContext context, Level level,
                                   ItemOwner owner, int seed, CallbackInfo ci) {
        if (RelicItems.isRelic(stack)) ((RelicRenderState) output).mistsmp$setRelic(true);
    }
}
package com.mistsmp.mod.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mistsmp.mod.client.relic.RelicGlint;
import com.mistsmp.mod.client.relic.RelicItems;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;

/** Worn relic armor and Dragonhide on the back: flag the relic while its layers are submitted. */
@Mixin(EquipmentLayerRenderer.class)
abstract class RelicEquipmentLayerMixin {

    @Unique
    private boolean mistsmp$previousActive;

    @Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("HEAD"))
    private <S> void mistsmp$beginRelic(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> asset, Model<? super S> model,
                                        S state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                        @Nullable Identifier textureOverride, int outline, int order, CallbackInfo ci) {
        mistsmp$previousActive = RelicGlint.setActive(RelicItems.isRelic(itemStack));
    }

    @Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At("RETURN"))
    private <S> void mistsmp$endRelic(EquipmentClientInfo.LayerType layerType, ResourceKey<EquipmentAsset> asset, Model<? super S> model,
                                      S state, ItemStack itemStack, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                      @Nullable Identifier textureOverride, int outline, int order, CallbackInfo ci) {
        RelicGlint.setActive(mistsmp$previousActive);
    }
}
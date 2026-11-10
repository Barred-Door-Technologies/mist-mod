package com.mistsmp.mod.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mistsmp.mod.client.cosmetic.CosmeticRenderState;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;

/** Adds cosmetic fields to vanilla's humanoid render state. */
@Mixin(HumanoidRenderState.class)
abstract class HumanoidRenderStateMixin implements CosmeticRenderState {

    @Unique
    private CosmeticTransform mistsmp$headTransform;

    @Unique
    private final ItemStackRenderState mistsmp$bodyItem = new ItemStackRenderState();

    @Unique
    private CosmeticTransform mistsmp$bodyTransform;

    @Unique
    private final ItemStackRenderState mistsmp$leftWing = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState mistsmp$rightWing = new ItemStackRenderState();

    @Unique
    private CosmeticTransform mistsmp$wingTransform;

    @Unique
    private Identifier mistsmp$capeTexture;

    @Unique
    private final ItemStackRenderState mistsmp$rightBoot = new ItemStackRenderState();

    @Unique
    private final ItemStackRenderState mistsmp$leftBoot = new ItemStackRenderState();

    @Unique
    private CosmeticTransform mistsmp$bootTransform;

    @Unique
    private final ItemStackRenderState mistsmp$faceItem = new ItemStackRenderState();

    @Unique
    private CosmeticTransform mistsmp$faceTransform;

    @Override
    public ItemStackRenderState mistsmp$getFaceItem() {
        return mistsmp$faceItem;
    }

    @Override
    public CosmeticTransform mistsmp$getFaceTransform() {
        return mistsmp$faceTransform;
    }

    @Override
    public void mistsmp$setFaceTransform(CosmeticTransform transform) {
        this.mistsmp$faceTransform = transform;
    }

    @Override
    public ItemStackRenderState mistsmp$getRightBoot() {
        return mistsmp$rightBoot;
    }

    @Override
    public ItemStackRenderState mistsmp$getLeftBoot() {
        return mistsmp$leftBoot;
    }

    @Override
    public CosmeticTransform mistsmp$getBootTransform() {
        return mistsmp$bootTransform;
    }

    @Override
    public void mistsmp$setBootTransform(CosmeticTransform transform) {
        this.mistsmp$bootTransform = transform;
    }

    @Override
    public CosmeticTransform mistsmp$getHeadTransform() {
        return mistsmp$headTransform;
    }

    @Override
    public void mistsmp$setHeadTransform(CosmeticTransform transform) {
        this.mistsmp$headTransform = transform;
    }

    @Override
    public ItemStackRenderState mistsmp$getBodyItem() {
        return mistsmp$bodyItem;
    }

    @Override
    public CosmeticTransform mistsmp$getBodyTransform() {
        return mistsmp$bodyTransform;
    }

    @Override
    public void mistsmp$setBodyTransform(CosmeticTransform transform) {
        this.mistsmp$bodyTransform = transform;
    }

    @Override
    public ItemStackRenderState mistsmp$getLeftWing() {
        return mistsmp$leftWing;
    }

    @Override
    public ItemStackRenderState mistsmp$getRightWing() {
        return mistsmp$rightWing;
    }

    @Override
    public CosmeticTransform mistsmp$getWingTransform() {
        return mistsmp$wingTransform;
    }

    @Override
    public void mistsmp$setWingTransform(CosmeticTransform transform) {
        this.mistsmp$wingTransform = transform;
    }

    @Override
    public Identifier mistsmp$getCapeTexture() {
        return mistsmp$capeTexture;
    }

    @Override
    public void mistsmp$setCapeTexture(Identifier texture) {
        this.mistsmp$capeTexture = texture;
    }
}
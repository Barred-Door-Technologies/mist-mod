package com.mistsmp.mod.client.cosmetic.follower;

import com.mistsmp.mod.client.cosmetic.CosmeticModels;
import com.mistsmp.mod.client.cosmetic.def.CosmeticAnimator;
import com.mistsmp.mod.client.cosmetic.def.CosmeticTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws followers in the world, not as part of the player, so you see your own follower in
 * first person and a follower stays visible when its owner is just off screen.
 *
 * Extraction (once per frame): interpolate position and facing, resolve the model frame, read the light.
 * Submit (same frame): draw the model there. Models are authored centered on (8,8,8), facing +Z.
 */
public final class FollowerRenderer {

    private static final Map<Identifier, ItemStack> STACKS = new ConcurrentHashMap<>();

    private FollowerRenderer() {
    }

    public static void init() {
        LevelExtractionEvents.END_EXTRACTION.register(FollowerRenderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(FollowerRenderer::submit);
    }

    private static void extract(LevelExtractionContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(false);

        for (FollowerTracker.State state : FollowerTracker.states()) {
            state.visible = false;
            if (state.position == null || state.owner == null || state.owner.isInvisible()) {
                continue;
            }

            Vec3 pos = state.previousPosition.lerp(state.position, partialTick);
            state.renderX = pos.x;
            state.renderY = pos.y;
            state.renderZ = pos.z;
            state.renderYaw = Mth.rotLerp(partialTick, state.previousYaw, state.yaw);
            state.light = LightCoordsUtil.getLightCoords(context.level(), BlockPos.containing(pos));

            Identifier frame = CosmeticAnimator.currentFrame(state.owner, state.definition);
            if (!frame.equals(state.cosmetic) && !CosmeticModels.exists(frame)) {
                frame = state.cosmetic;
            }
            minecraft.getItemModelResolver().updateForLiving(state.item, stackFor(frame), ItemDisplayContext.NONE, state.owner);
            state.transform = CosmeticTransform.compute(state.definition, state.owner, partialTick);
            state.visible = !state.item.isEmpty();
        }
    }

    private static void submit(LevelRenderContext context) {
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();

        for (FollowerTracker.State state : FollowerTracker.states()) {
            if (!state.visible) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(state.renderX - camera.x, state.renderY - camera.y, state.renderZ - camera.z);
            // Model +Z points the way the follower faces.
            poseStack.rotateDegrees(Axis.YP, -state.renderYaw);
            float scale = state.settings.scale();
            poseStack.scale(scale, scale, scale);
            if (state.transform != null) {
                state.transform.applyTo(poseStack);
            }
            state.item.submit(poseStack, context.submitNodeCollector(), state.light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static ItemStack stackFor(Identifier cosmetic) {
        return STACKS.computeIfAbsent(cosmetic, id -> {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponents.ITEM_MODEL, id);
            return stack;
        });
    }
}
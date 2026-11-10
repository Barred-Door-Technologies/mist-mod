package com.mistsmp.mod.client;

import com.mistsmp.mod.client.config.MistSettings;
import com.mistsmp.mod.client.cosmetic.BodyCosmeticLayer;
import com.mistsmp.mod.client.cosmetic.CosmeticModels;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.cosmetic.follower.FollowerRenderer;
import com.mistsmp.mod.client.cosmetic.follower.FollowerTracker;
import com.mistsmp.mod.client.cosmetic.particle.CosmeticParticles;
import com.mistsmp.mod.client.network.ClientNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class MistModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MistSettings.load();
        ClientNetworking.init();
        CosmeticModels.init();
        CosmeticDefinitions.init();
        MistKeys.init();
        FollowerRenderer.init();

        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) {
                helper.register(new BodyCosmeticLayer(avatar));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MistKeys.tick(client);
            FollowerTracker.tick(client.level);
            CosmeticParticles.tick(client);
        });
    }
}
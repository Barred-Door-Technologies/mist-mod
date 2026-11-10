package com.mistsmp.mod.client;

import org.lwjgl.sdl.SDLScancode;

import com.mistsmp.mod.client.config.MistSettings;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Key binds under "Mist SMP" in Options > Controls > Key Binds. Players can remap them there.
 * K: show or hide other players' cosmetic particles (your own are unaffected).
 */
public final class MistKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mistsmp", "main"));

    private static KeyMapping toggleParticles;

    private MistKeys() {
    }

    public static void init() {
        toggleParticles = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mistsmp.toggle_particles", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_K, CATEGORY));
    }

    public static void tick(Minecraft minecraft) {
        while (toggleParticles.consumeClick()) {
            boolean show = !MistSettings.get().showOtherPlayersParticles();
            MistSettings.update(new MistSettings(show));
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(
                        show ? "message.mistsmp.particles_shown" : "message.mistsmp.particles_hidden"));
            }
        }
    }
}
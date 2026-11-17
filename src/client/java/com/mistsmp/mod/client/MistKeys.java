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
 * Unbound by default: reduce motion on animated titles (they show as a still gradient).
 */
public final class MistKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("mistsmp", "main"));

    private static KeyMapping toggleParticles;
    private static KeyMapping toggleTitleMotion;

    private MistKeys() {
    }

    public static void init() {
        toggleParticles = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mistsmp.toggle_particles", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_K, CATEGORY));
        toggleTitleMotion = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mistsmp.toggle_title_motion", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }

    public static void tick(Minecraft minecraft) {
        while (toggleParticles.consumeClick()) {
            MistSettings settings = MistSettings.get();
            boolean show = !settings.showOtherPlayersParticles();
            MistSettings.update(new MistSettings(show, settings.reduceTitleMotion()));
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(
                        show ? "message.mistsmp.particles_shown" : "message.mistsmp.particles_hidden"));
            }
        }

        while (toggleTitleMotion.consumeClick()) {
            MistSettings settings = MistSettings.get();
            boolean reduce = !settings.reduceTitleMotion();
            MistSettings.update(new MistSettings(settings.showOtherPlayersParticles(), reduce));
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(
                        reduce ? "message.mistsmp.title_motion_reduced" : "message.mistsmp.title_motion_on"));
            }
        }
    }
}
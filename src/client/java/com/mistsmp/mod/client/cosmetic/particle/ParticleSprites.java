package com.mistsmp.mod.client.cosmetic.particle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Looks up custom particle textures in the game's particle atlas by name.
 *
 * The particle atlas collects every PNG under assets/<namespace>/textures/particle/ from every loaded pack,
 * so a texture in our resource pack is available here with no registration. Sprites are looked up at spawn
 * time rather than cached, because the atlas is rebuilt whenever resources reload (F3+T, pack changes).
 *
 * This is the only class that touches the atlas API, so a Minecraft rename only needs fixing here.
 */
final class ParticleSprites {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Set<Identifier> WARNED = new HashSet<>();

    private ParticleSprites() {
    }

    /** @return the sprites that exist, in order; empty when none are in the atlas (logged once per name). */
    static List<TextureAtlasSprite> resolve(List<Identifier> ids) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
        List<TextureAtlasSprite> sprites = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            TextureAtlasSprite sprite = atlas.getSprite(id);
            if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                if (WARNED.add(id)) {
                    LOGGER.warn("Particle texture {} is not in the particle atlas; expected assets/{}/textures/particle/{}.png",
                            id, id.getNamespace(), id.getPath());
                }
                continue;
            }
            sprites.add(sprite);
        }
        return sprites;
    }
}

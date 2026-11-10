package com.mistsmp.mod.client.cosmetic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Checks whether a texture file exists in the loaded resource packs. Results are cached and the cache
 * is cleared on every resource reload (CosmeticDefinitions calls clear()).
 */
public final class CosmeticTextures {

    private static final Map<Identifier, Boolean> EXISTS = new ConcurrentHashMap<>();

    private CosmeticTextures() {
    }

    /** mistsmp:cosmetic/back/test_cape -> mistsmp:textures/entity/cosmetic/back/test_cape.png */
    public static Identifier entityTexture(Identifier cosmetic) {
        return cosmetic.withPath(path -> "textures/entity/" + path + ".png");
    }

    public static boolean exists(Identifier texturePath) {
        return EXISTS.computeIfAbsent(texturePath,
                path -> Minecraft.getInstance().getResourceManager().getResource(path).isPresent());
    }

    public static void clear() {
        EXISTS.clear();
    }
}
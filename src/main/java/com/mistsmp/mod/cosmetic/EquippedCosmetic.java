package com.mistsmp.mod.cosmetic;

import java.util.Map;

import net.minecraft.resources.Identifier;

/**
 * One cosmetic a player has equipped, plus optional parameters from the server.
 * Parameters are free-form strings so the server can add new ones without a mod update;
 * code that doesn't know a key ignores it. Known keys:
 *   tint  hex color like #FFAA00, used by particle and texture tints (supporter auras)
 */
public record EquippedCosmetic(Identifier id, Map<String, String> params) {

    public EquippedCosmetic {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key) {
        return params.get(key);
    }
}
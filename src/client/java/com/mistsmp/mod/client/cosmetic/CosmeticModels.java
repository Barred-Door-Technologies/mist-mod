package com.mistsmp.mod.client.cosmetic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * A cosmetic id like mistsmp:cosmetic/head/top_hat must have an item definition at
 * assets/mistsmp/items/cosmetic/head/top_hat.json. If it doesn't (pack not accepted yet,
 * outdated pack, typo in the database), the slot renders vanilla instead of the
 * missing-model cube.
 *
 * exists() logs a missing model once; has() is the same check without the log line, for places
 * where a missing model is normal (texture-style cosmetics, optional _left/_right files).
 *
 * Results are cached and the cache is cleared on every resource reload, which includes
 * the server resource pack being applied after join.
 */
public final class CosmeticModels {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");

    private static final Map<Identifier, Boolean> EXISTS = new ConcurrentHashMap<>();
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    private CosmeticModels() {
    }

    public static void init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("mistsmp", "cosmetic_models"),
                (ResourceManagerReloadListener) manager -> {
                    EXISTS.clear();
                    WARNED.clear();
                });
    }

    /** Model check that logs once when the model is missing. */
    public static boolean exists(Identifier cosmeticId) {
        boolean found = has(cosmeticId);
        if (!found && WARNED.add(cosmeticId)) {
            LOGGER.warn("No model for cosmetic {} in the loaded resource packs; rendering vanilla instead", cosmeticId);
        }
        return found;
    }

    /** Model check with no log line. */
    public static boolean has(Identifier cosmeticId) {
        return EXISTS.computeIfAbsent(cosmeticId, id -> {
            Identifier definition = Identifier.fromNamespaceAndPath(id.getNamespace(), "items/" + id.getPath() + ".json");
            return Minecraft.getInstance().getResourceManager().getResource(definition).isPresent();
        });
    }
}
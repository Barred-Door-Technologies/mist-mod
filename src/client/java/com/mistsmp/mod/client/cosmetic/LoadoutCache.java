package com.mistsmp.mod.client.cosmetic;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mistsmp.mod.cosmetic.CosmeticSlot;
import com.mistsmp.mod.cosmetic.EquippedCosmetic;
import com.mistsmp.mod.network.LoadoutsPayload;

import net.minecraft.resources.Identifier;

/**
 * Client-side copy of every player's cosmetic loadout, as told by the server.
 * The server is the only source of truth. This cache is cleared on disconnect.
 */
public final class LoadoutCache {

    private static final Map<UUID, Map<CosmeticSlot, EquippedCosmetic>> LOADOUTS = new ConcurrentHashMap<>();

    private LoadoutCache() {
    }

    public static void apply(LoadoutsPayload payload) {
        if (payload.replaceAll()) {
            LOADOUTS.clear();
        }
        for (LoadoutsPayload.Entry entry : payload.entries()) {
            if (entry.cosmetics().isEmpty()) {
                LOADOUTS.remove(entry.player());
            } else {
                LOADOUTS.put(entry.player(), entry.cosmetics());
            }
        }
    }

    /** @return the equipped cosmetic (id and params) for this player and slot, or null if none. */
    public static EquippedCosmetic getEquipped(UUID player, CosmeticSlot slot) {
        Map<CosmeticSlot, EquippedCosmetic> loadout = LOADOUTS.get(player);
        return loadout == null ? null : loadout.get(slot);
    }

    /** @return the cosmetic id for this player and slot, or null if none. */
    public static Identifier get(UUID player, CosmeticSlot slot) {
        EquippedCosmetic equipped = getEquipped(player, slot);
        return equipped == null ? null : equipped.id();
    }

    public static Map<UUID, Map<CosmeticSlot, EquippedCosmetic>> snapshot() {
        return Collections.unmodifiableMap(LOADOUTS);
    }

    public static void clear() {
        LOADOUTS.clear();
    }
}
package com.mistsmp.mod.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mistsmp.mod.cosmetic.CosmeticSlot;
import com.mistsmp.mod.cosmetic.EquippedCosmetic;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client. Cosmetic loadouts for one or more players.
 *
 * replaceAll = true: full snapshot, the client drops everything it had first (sent on join).
 * replaceAll = false: delta, each listed player's entry replaces that player's previous entry.
 * An entry with zero cosmetics clears that player.
 *
 * Wire format (protocol 2, see docs/PROTOCOL.md):
 *   Boolean replaceAll
 *   VarInt entryCount
 *   entryCount x { UUID player, VarInt cosmeticCount,
 *                  cosmeticCount x { VarInt slotWireId, String cosmeticId,
 *                                    VarInt paramCount, paramCount x { String key, String value } } }
 */
public record LoadoutsPayload(boolean replaceAll, List<Entry> entries) implements CustomPacketPayload {

    /** Upper bounds so a malformed packet cannot make the client allocate absurd amounts. */
    private static final int MAX_ENTRIES = 1024;
    private static final int MAX_COSMETICS_PER_PLAYER = 16;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_PARAMS = 16;
    private static final int MAX_PARAM_KEY_LENGTH = 64;
    private static final int MAX_PARAM_VALUE_LENGTH = 256;

    public static final CustomPacketPayload.Type<LoadoutsPayload> TYPE =
            new CustomPacketPayload.Type<>(MistProtocol.id("loadouts"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutsPayload> CODEC =
            CustomPacketPayload.codec(LoadoutsPayload::write, LoadoutsPayload::read);

    public record Entry(UUID player, Map<CosmeticSlot, EquippedCosmetic> cosmetics) {
    }

    private static LoadoutsPayload read(RegistryFriendlyByteBuf buf) {
        boolean replaceAll = buf.readBoolean();
        int entryCount = buf.readVarInt();
        if (entryCount < 0 || entryCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid loadout entry count: " + entryCount);
        }

        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            UUID player = new UUID(buf.readLong(), buf.readLong());
            int cosmeticCount = buf.readVarInt();
            if (cosmeticCount < 0 || cosmeticCount > MAX_COSMETICS_PER_PLAYER) {
                throw new IllegalArgumentException("Invalid cosmetic count: " + cosmeticCount);
            }

            Map<CosmeticSlot, EquippedCosmetic> cosmetics = new EnumMap<>(CosmeticSlot.class);
            for (int j = 0; j < cosmeticCount; j++) {
                int slotWireId = buf.readVarInt();
                String rawId = buf.readUtf(MAX_ID_LENGTH);
                Map<String, String> params = readParams(buf);

                // Always read every field before deciding to skip, so the buffer stays aligned.
                CosmeticSlot slot = CosmeticSlot.fromWireId(slotWireId);
                Identifier id = Identifier.tryParse(rawId);
                if (slot != null && id != null) {
                    cosmetics.put(slot, new EquippedCosmetic(id, params));
                }
            }
            entries.add(new Entry(player, Collections.unmodifiableMap(cosmetics)));
        }
        return new LoadoutsPayload(replaceAll, Collections.unmodifiableList(entries));
    }

    private static Map<String, String> readParams(RegistryFriendlyByteBuf buf) {
        int paramCount = buf.readVarInt();
        if (paramCount < 0 || paramCount > MAX_PARAMS) {
            throw new IllegalArgumentException("Invalid param count: " + paramCount);
        }
        if (paramCount == 0) {
            return Map.of();
        }
        Map<String, String> params = new HashMap<>(paramCount);
        for (int k = 0; k < paramCount; k++) {
            String key = buf.readUtf(MAX_PARAM_KEY_LENGTH);
            String value = buf.readUtf(MAX_PARAM_VALUE_LENGTH);
            params.put(key, value);
        }
        return params;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(replaceAll);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeLong(entry.player().getMostSignificantBits());
            buf.writeLong(entry.player().getLeastSignificantBits());
            buf.writeVarInt(entry.cosmetics().size());
            for (Map.Entry<CosmeticSlot, EquippedCosmetic> cosmetic : entry.cosmetics().entrySet()) {
                buf.writeVarInt(cosmetic.getKey().wireId());
                buf.writeUtf(cosmetic.getValue().id().toString());
                Map<String, String> params = cosmetic.getValue().params();
                buf.writeVarInt(params.size());
                for (Map.Entry<String, String> param : params.entrySet()) {
                    buf.writeUtf(param.getKey());
                    buf.writeUtf(param.getValue());
                }
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
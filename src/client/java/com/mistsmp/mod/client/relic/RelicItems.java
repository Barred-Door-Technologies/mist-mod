package com.mistsmp.mod.client.relic;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Recognises Mistforged Relics. The server's plugin tags every relic with "mistsmp:legendary"
 * (in the plugin, "legendary" means Relic), which Paper stores under PublicBukkitValues in the
 * item's custom data. Custom data is sent to clients with the item, so other players' relics are
 * recognised too.
 */
public final class RelicItems {

    private static final String PAPER_VALUES = "PublicBukkitValues";
    private static final String RELIC_KEY = "mistsmp:legendary";
    private static final String BAG_ID = "bag_of_holding";
    private static final String BAG_COLLECTING_KEY = "mistsmp:bag_collecting";

    private enum State { NONE, RELIC, DORMANT }

    /** Custom data is immutable and shared between copies of a stack, so the answer can be cached. Client thread only. */
    private static final Map<CustomData, State> CACHE = new WeakHashMap<>();

    private RelicItems() {
    }

    /** True for any relic, including a Bag of Holding that isn't collecting. */
    public static boolean isRelic(ItemStack stack) {
        return state(stack) != State.NONE;
    }

    /** True when the relic should show its particles. A Bag of Holding only does while it's collecting. */
    public static boolean showsEffects(ItemStack stack) {
        return state(stack) == State.RELIC;
    }

    private static State state(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return State.NONE;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return State.NONE;
        return CACHE.computeIfAbsent(data, RelicItems::readTag);
    }

    private static State readTag(CustomData data) {
        CompoundTag tag = data.copyTag();
        return tag.getCompound(PAPER_VALUES).map(values -> {
            String id = values.getString(RELIC_KEY).orElse(null);
            if (id == null) return State.NONE;
            if (BAG_ID.equals(id) && values.getByteOr(BAG_COLLECTING_KEY, (byte) 0) == 0) return State.DORMANT;
            return State.RELIC;
        }).orElse(State.NONE);
    }
}
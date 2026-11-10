package com.mistsmp.mod.client.cosmetic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinition;
import com.mistsmp.mod.client.cosmetic.def.CosmeticDefinitions;
import com.mistsmp.mod.client.network.ClientSession;
import com.mistsmp.mod.cosmetic.CosmeticSlot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * HELD slot: replaces the model of a held item, in hand only (first and third person). Inventory icons,
 * dropped items, and item frames keep the real model, so players always know what an item really is.
 *
 * The definition lists which items it applies to; with no list it applies to nothing:
 *   "held": { "appliesTo": [ "minecraft:diamond_sword", "#minecraft:swords" ] }   (# = item tag)
 *
 * The model is an ordinary item model (items/cosmetic/held/<key>.json) with Display tab hand settings.
 * The held item keeps its other data, so enchantment glint still shows. Flipbook frames work.
 * Called from ItemModelResolverMixin, which every hand item passes through.
 */
public final class HeldCosmetics {

    private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");
    private static final Set<ItemDisplayContext> HAND_CONTEXTS = EnumSet.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);

    /** Parsed appliesTo lists; definitions are replaced on reload, so a reloaded file is parsed again. */
    private static final Map<CosmeticDefinition, Matcher> MATCHERS = new HashMap<>();
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();

    private HeldCosmetics() {
    }

    /** @return the stack to resolve the model from: the original, or a copy wearing the cosmetic model. */
    public static ItemStack replace(ItemStack stack, ItemDisplayContext context, ItemOwner owner) {
        if (stack.isEmpty() || owner == null || !HAND_CONTEXTS.contains(context) || !ClientSession.isWelcomed()) {
            return stack;
        }
        LivingEntity entity = owner.asLivingEntity();
        if (!(entity instanceof Player)) {
            return stack;
        }
        Identifier cosmetic = LoadoutCache.get(entity.getUUID(), CosmeticSlot.HELD);
        if (cosmetic == null || !CosmeticModels.exists(cosmetic)) {
            return stack;
        }
        CosmeticDefinition definition = CosmeticDefinitions.get(cosmetic);
        if (!matcherFor(definition).matches(stack)) {
            return stack;
        }
        Identifier frame = HeadCosmetics.frameFor(entity, cosmetic, definition);
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ITEM_MODEL, frame);
        return copy;
    }

    private static Matcher matcherFor(CosmeticDefinition definition) {
        if (MATCHERS.size() > 256) MATCHERS.clear();
        return MATCHERS.computeIfAbsent(definition, HeldCosmetics::parse);
    }

    private static Matcher parse(CosmeticDefinition definition) {
        List<Identifier> items = new ArrayList<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        JsonObject raw = definition.raw();
        if (raw.has("held") && raw.get("held").isJsonObject()) {
            JsonObject held = raw.getAsJsonObject("held");
            if (held.has("appliesTo") && held.get("appliesTo").isJsonArray()) {
                for (JsonElement element : held.getAsJsonArray("appliesTo")) {
                    if (!element.isJsonPrimitive()) continue;
                    String entry = element.getAsString().trim();
                    boolean isTag = entry.startsWith("#");
                    Identifier id = Identifier.tryParse(isTag ? entry.substring(1) : entry);
                    if (id == null) continue;
                    if (isTag) {
                        tags.add(TagKey.create(Registries.ITEM, id));
                    } else {
                        items.add(id);
                    }
                }
            }
        }
        if (items.isEmpty() && tags.isEmpty() && WARNED.add(definition.id())) {
            LOGGER.warn("{}: HELD cosmetic has no held.appliesTo list, so it applies to no items", definition.id());
        }
        return new Matcher(List.copyOf(items), List.copyOf(tags));
    }

    private record Matcher(List<Identifier> items, List<TagKey<Item>> tags) {
        boolean matches(ItemStack stack) {
            for (Identifier id : items) {
                if (stack.typeHolder().is(id)) return true;
            }
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) return true;
            }
            return false;
        }
    }
}
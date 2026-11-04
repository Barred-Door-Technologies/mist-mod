package com.mistsmp.mod.cosmetic;

/**
 * Cosmetic slots. The wire id is explicit so reordering or adding constants
 * never changes what an existing id means. Never reuse a retired id.
 *
 * Every slot renders only while something is in it (the vanilla item is hidden and the
 * cosmetic is drawn instead). FACE, FOLLOWER and AURA have no vanilla item and show whenever equipped.
 */
public enum CosmeticSlot {
    HEAD(0),
    CHEST(1),
    LEGS(2),
    FEET(3),
    BACK(4),
    HELD(5),
    FACE(6),
    FOLLOWER(7),
    AURA(8);

    private final int wireId;

    CosmeticSlot(int wireId) {
        this.wireId = wireId;
    }

    public int wireId() {
        return wireId;
    }

    /**
     * @return the slot for this wire id, or null if this client does not know it
     *         (a newer server added a slot). Unknown slots are skipped, not errors.
     */
    public static CosmeticSlot fromWireId(int wireId) {
        for (CosmeticSlot slot : values()) {
            if (slot.wireId == wireId) {
                return slot;
            }
        }
        return null;
    }
}
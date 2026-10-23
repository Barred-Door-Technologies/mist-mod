package com.mistsmp.mod.cosmetic;

/**
 * Cosmetic slots. The wire id is explicit so reordering or adding constants
 * never changes what an existing id means. Never reuse a retired id.
 */
public enum CosmeticSlot {
	HEAD(0),
	WINGS(1);

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

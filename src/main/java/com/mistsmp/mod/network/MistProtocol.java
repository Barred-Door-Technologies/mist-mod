package com.mistsmp.mod.network;

import net.minecraft.resources.Identifier;

/**
 * Shared constants for the plugin <-> mod protocol.
 * The wire format is documented in docs/PROTOCOL.md, which is the contract
 * mist-plugin implements on the server side. Change both together.
 */
public final class MistProtocol {

	public static final String NAMESPACE = "mistsmp";

	/**
	 * Bump only when the wire format changes in a way old clients cannot read.
	 * Mod releases that change rendering or assets keep the same protocol version.
	 */
	public static final int PROTOCOL_VERSION = 1;

	private MistProtocol() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}
}

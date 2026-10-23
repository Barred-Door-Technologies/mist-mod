package com.mistsmp.mod.client.network;

/**
 * Whether the current server is a Mist SMP server that accepted our hello.
 * Everything the mod renders is gated on this, so the mod stays inert elsewhere.
 */
public final class ClientSession {

	private static volatile boolean helloSent;
	private static volatile boolean welcomed;

	private ClientSession() {
	}

	public static boolean isWelcomed() {
		return welcomed;
	}

	static boolean markHelloSent() {
		if (helloSent) {
			return false;
		}
		helloSent = true;
		return true;
	}

	static void markWelcomed() {
		welcomed = true;
	}

	static void reset() {
		helloSent = false;
		welcomed = false;
	}
}

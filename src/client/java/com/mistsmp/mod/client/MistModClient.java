package com.mistsmp.mod.client;

import com.mistsmp.mod.client.network.ClientNetworking;

import net.fabricmc.api.ClientModInitializer;

public class MistModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientNetworking.init();
		// Cosmetic rendering (HEAD, WINGS) is registered here once implemented.
	}
}

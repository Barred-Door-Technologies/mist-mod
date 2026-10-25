package com.mistsmp.mod.client;

import com.mistsmp.mod.client.cosmetic.CosmeticModels;
import com.mistsmp.mod.client.network.ClientNetworking;

import net.fabricmc.api.ClientModInitializer;

public class MistModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
        ClientNetworking.init();
        CosmeticModels.init();
	}
}

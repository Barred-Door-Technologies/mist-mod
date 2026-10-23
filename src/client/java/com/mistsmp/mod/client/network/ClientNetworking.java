package com.mistsmp.mod.client.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mistsmp.mod.client.cosmetic.LoadoutCache;
import com.mistsmp.mod.network.HelloPayload;
import com.mistsmp.mod.network.LoadoutsPayload;
import com.mistsmp.mod.network.MistProtocol;
import com.mistsmp.mod.network.WelcomePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Handshake flow:
 *  1. The Mist plugin registers the mistsmp:hello channel when a player joins.
 *  2. When the client sees that registration, it sends HelloPayload. Servers that
 *     never register the channel never hear from this mod.
 *  3. The plugin replies with WelcomePayload, or disconnects outdated clients.
 *  4. The plugin then sends a full LoadoutsPayload snapshot, followed by deltas.
 */
public final class ClientNetworking {

	private static final Logger LOGGER = LoggerFactory.getLogger("mistsmp");

	private ClientNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(WelcomePayload.TYPE, WelcomePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LoadoutsPayload.TYPE, LoadoutsPayload.CODEC);

		// Step 2: only speak once the server has registered our channel.
		ServerboundPlayChannelEvents.REGISTER.register((listener, sender, client, channels) -> {
			if (channels.contains(HelloPayload.TYPE.id()) && ClientSession.markHelloSent()) {
				sender.sendPacket(new HelloPayload(MistProtocol.PROTOCOL_VERSION, modVersion()));
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(WelcomePayload.TYPE, (payload, context) -> {
			ClientSession.markWelcomed();
			LOGGER.info("Mist SMP server accepted client (server protocol {})", payload.serverProtocolVersion());
		});

		ClientPlayNetworking.registerGlobalReceiver(LoadoutsPayload.TYPE, (payload, context) -> {
			if (!ClientSession.isWelcomed()) {
				return;
			}
			LoadoutCache.apply(payload);
			LOGGER.debug("Applied loadouts: replaceAll={}, entries={}", payload.replaceAll(), payload.entries().size());
		});

		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientSession.reset();
			LoadoutCache.clear();
		});
	}

	private static String modVersion() {
		return FabricLoader.getInstance()
				.getModContainer("mistsmp")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
	}
}

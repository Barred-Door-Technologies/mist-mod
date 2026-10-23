package com.mistsmp.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client. The server accepted the hello. Clients that are rejected
 * are disconnected by the server instead of receiving this.
 * Wire format: VarInt serverProtocolVersion.
 */
public record WelcomePayload(int serverProtocolVersion) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<WelcomePayload> TYPE =
			new CustomPacketPayload.Type<>(MistProtocol.id("welcome"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WelcomePayload> CODEC =
			CustomPacketPayload.codec(WelcomePayload::write, WelcomePayload::new);

	private WelcomePayload(RegistryFriendlyByteBuf buf) {
		this(buf.readVarInt());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(serverProtocolVersion);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

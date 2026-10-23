package com.mistsmp.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server. Sent once the server registers the mistsmp:hello channel.
 * Wire format: VarInt protocolVersion, String modVersion.
 */
public record HelloPayload(int protocolVersion, String modVersion) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<HelloPayload> TYPE =
			new CustomPacketPayload.Type<>(MistProtocol.id("hello"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC =
			CustomPacketPayload.codec(HelloPayload::write, HelloPayload::new);

	private HelloPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readUtf());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(protocolVersion);
		buf.writeUtf(modVersion);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

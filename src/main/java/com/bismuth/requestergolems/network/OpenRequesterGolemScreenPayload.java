package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterGolems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenRequesterGolemScreenPayload(int publicId, String displayName, boolean canEdit, boolean canClaim) implements CustomPacketPayload {
	public static final Type<OpenRequesterGolemScreenPayload> TYPE =
			new Type<>(RequesterGolems.id("open_requester_golem_screen"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenRequesterGolemScreenPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, OpenRequesterGolemScreenPayload::publicId,
					ByteBufCodecs.STRING_UTF8, OpenRequesterGolemScreenPayload::displayName,
					ByteBufCodecs.BOOL, OpenRequesterGolemScreenPayload::canEdit,
					ByteBufCodecs.BOOL, OpenRequesterGolemScreenPayload::canClaim,
					OpenRequesterGolemScreenPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

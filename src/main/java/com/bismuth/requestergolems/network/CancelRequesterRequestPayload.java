package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterGolems;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CancelRequesterRequestPayload(int requestIndex) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CancelRequesterRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(RequesterGolems.id("cancel_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CancelRequesterRequestPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT,
					CancelRequesterRequestPayload::requestIndex,
					CancelRequesterRequestPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (!(context.player().containerMenu instanceof RequesterChestMenu menu)) {
				return;
			}

			if (payload.requestIndex() < 0
					|| payload.requestIndex() >= RequesterChestMenu.ACTIVE_REQUEST_SLOT_COUNT) {
				return;
			}

			menu.requestergolems$cancelActiveRequest(payload.requestIndex());
		});
	}
}

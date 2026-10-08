package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterGolems;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record SetRequesterRequestPayload(int requestIndex, ItemStack stack) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetRequesterRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(RequesterGolems.id("set_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SetRequesterRequestPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT,
					SetRequesterRequestPayload::requestIndex,
					ItemStack.OPTIONAL_STREAM_CODEC,
					SetRequesterRequestPayload::stack,
					SetRequesterRequestPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (!(context.player().containerMenu instanceof RequesterChestMenu menu)) {
				RequesterGolems.LOGGER.warn("Request update rejected: player is not in RequesterChestMenu");
				return;
			}

			if (payload.requestIndex() < 0 || payload.requestIndex() >= RequesterChestMenu.REQUEST_SLOT_COUNT) {
				RequesterGolems.LOGGER.warn("Request update rejected: invalid request index {}", payload.requestIndex());
				return;
			}

			ItemStack requested = payload.stack();
			if (!requested.isEmpty() && requested.getCount() > requested.getMaxStackSize()) {
				RequesterGolems.LOGGER.warn("Request update rejected: invalid stack count {}", requested.getCount());
				return;
			}

			menu.requestergolems$setRequestFromClient(payload.requestIndex(), requested);
		});
	}
}

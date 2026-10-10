package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterGolems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequesterAssignmentsListPayload(String assignedGolems) implements CustomPacketPayload {
	public static final Type<RequesterAssignmentsListPayload> TYPE =
			new Type<>(RequesterGolems.id("requester_assignments_list"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequesterAssignmentsListPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RequesterAssignmentsListPayload::assignedGolems,
					RequesterAssignmentsListPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

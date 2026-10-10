package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterGolemAccess;
import com.bismuth.requestergolems.RequesterGolemLookup;
import com.bismuth.requestergolems.RequesterGolems;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.golem.CopperGolem;

public record UnassignAllRequesterGolemPayload(int publicId) implements CustomPacketPayload {
	public static final Type<UnassignAllRequesterGolemPayload> TYPE =
			new Type<>(RequesterGolems.id("unassign_all_requester_golem"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UnassignAllRequesterGolemPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, UnassignAllRequesterGolemPayload::publicId,
					UnassignAllRequesterGolemPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (payload.publicId() <= 0) return;
			var player = context.player();
			var level = (net.minecraft.server.level.ServerLevel) player.level();
			List<CopperGolem> matches = RequesterGolemLookup.findLoaded(level.getServer(), "#" + payload.publicId(), 2);
			if (matches.size() != 1) return;
			CopperGolem golem = matches.getFirst();
			if (!(golem instanceof RequesterGolemAccess access)
					|| access.requestergolems$getOwner() == null
					|| !access.requestergolems$getOwner().equals(player.getUUID())) {
				player.sendSystemMessage(Component.literal("Only this Requester Golem's owner can change its assignments."));
				return;
			}
			access.requestergolems$clearAssignments();

		});
	}
}

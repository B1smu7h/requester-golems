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

public record RenameRequesterGolemPayload(int publicId, String displayName) implements CustomPacketPayload {
	public static final Type<RenameRequesterGolemPayload> TYPE =
			new Type<>(RequesterGolems.id("rename_requester_golem"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RenameRequesterGolemPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, RenameRequesterGolemPayload::publicId,
					ByteBufCodecs.STRING_UTF8, RenameRequesterGolemPayload::displayName,
					RenameRequesterGolemPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (payload.publicId() <= 0 || payload.displayName() == null || payload.displayName().length() > 128) return;
			var player = context.player();
			var level = (net.minecraft.server.level.ServerLevel) player.level();
			var matches = RequesterGolemLookup.findLoaded(level.getServer(), "#" + payload.publicId(), 2);
			if (matches.size() != 1) return;
			CopperGolem golem = matches.getFirst();
			if (!(golem instanceof RequesterGolemAccess access)
					|| access.requestergolems$getOwner() == null
					|| !access.requestergolems$getOwner().equals(player.getUUID())) {
				player.sendSystemMessage(Component.literal("Only this Requester Golem's owner can rename it."));
				return;
			}
			String cleaned = payload.displayName().strip().replaceAll("[\\p{Cntrl}]", "");
			if (cleaned.isEmpty()) {
				player.sendSystemMessage(Component.literal("A golem name cannot be blank."));
				return;
			}
			if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
			access.requestergolems$setDisplayName(cleaned);
			player.sendSystemMessage(Component.literal("Requester Golem renamed to " + cleaned + "."));
		});
	}
}

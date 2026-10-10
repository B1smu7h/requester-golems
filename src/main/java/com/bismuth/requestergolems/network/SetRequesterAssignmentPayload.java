package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import com.bismuth.requestergolems.RequesterGolemLookup;
import com.bismuth.requestergolems.RequesterGolems;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public record SetRequesterAssignmentPayload(String query, boolean assigned) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetRequesterAssignmentPayload> TYPE =
			new CustomPacketPayload.Type<>(RequesterGolems.id("set_requester_assignment"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SetRequesterAssignmentPayload> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.STRING_UTF8, SetRequesterAssignmentPayload::query,
					ByteBufCodecs.BOOL, SetRequesterAssignmentPayload::assigned,
					SetRequesterAssignmentPayload::new
			);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			var player = context.player();
			if (payload.query() == null || payload.query().strip().isEmpty() || payload.query().length() > 32) {
				player.sendSystemMessage(Component.literal("Enter a golem name or public ID (for example #000042)."));
				return;
			}
			if (!(player.containerMenu instanceof RequesterChestMenu menu)) return;
			ChestBlockEntity chest = menu.requestergolems$getRequesterChest();
			if (chest == null || !(chest instanceof RequesterChestAccess chestAccess)
					|| !chestAccess.requestergolems$isRequester()
					|| !(chest.getLevel() instanceof ServerLevel chestLevel)) return;

			var server = chestLevel.getServer();
			if (server == null) return;
			List<CopperGolem> matches = RequesterGolemLookup.findLoaded(server, payload.query(), 2);
			if (matches.isEmpty()) {
				player.sendSystemMessage(Component.literal("No loaded Requester Golem matched that name or ID."));
				return;
			}
			if (matches.size() != 1) {
				player.sendSystemMessage(Component.literal("That name matches multiple golems. Use the unique public ID instead."));
				return;
			}

			CopperGolem golem = matches.getFirst();
			if (!(golem instanceof RequesterGolemAccess golemAccess)
					|| golemAccess.requestergolems$getOwner() == null
					|| !golemAccess.requestergolems$getOwner().equals(player.getUUID())) {
				player.sendSystemMessage(Component.literal("You can only assign Requester Golems that you own. Legacy golems must be claimed first (sneak + empty-hand right-click)."));
				return;
			}

			golemAccess.requestergolems$setAssignedTo(
					chestLevel.dimension().identifier().toString(),
					chestAccess.requestergolems$getIdentity(),
					payload.assigned()
			);
			player.sendSystemMessage(Component.literal(
					(payload.assigned() ? "Assigned " : "Unassigned ")
							+ golemAccess.requestergolems$getDisplayName()
							+ " (#" + String.format(java.util.Locale.ROOT, "%06d", golemAccess.requestergolems$getPublicId()) + ") "
							+ (payload.assigned() ? "exclusively to this Requester Chest." : "from this Requester Chest.")
			));
		});
	}
}

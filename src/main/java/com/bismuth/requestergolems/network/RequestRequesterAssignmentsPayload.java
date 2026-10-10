package com.bismuth.requestergolems.network;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import com.bismuth.requestergolems.RequesterGolems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import com.bismuth.requestergolems.menu.RequesterChestMenu;

public record RequestRequesterAssignmentsPayload() implements CustomPacketPayload {
	public static final Type<RequestRequesterAssignmentsPayload> TYPE =
			new Type<>(RequesterGolems.id("request_requester_assignments"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestRequesterAssignmentsPayload> CODEC =
			StreamCodec.unit(new RequestRequesterAssignmentsPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }

	public static void registerServerReceiver() {
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			var player = context.player();
			if (!(player.containerMenu instanceof RequesterChestMenu menu)) return;
			ChestBlockEntity chest = menu.requestergolems$getRequesterChest();
			if (!(chest instanceof RequesterChestAccess chestAccess)
					|| !chestAccess.requestergolems$isRequester()
					|| !(chest.getLevel() instanceof ServerLevel level)
					|| level.getBlockEntity(chest.getBlockPos()) != chest) return;

			String dimension = level.dimension().identifier().toString();
			var chestId = chestAccess.requestergolems$getIdentity();
			List<CopperGolem> assigned = new ArrayList<>();
			for (ServerLevel loadedLevel : level.getServer().getAllLevels()) {
				for (Entity entity : loadedLevel.getAllEntities()) {
					if (entity instanceof CopperGolem golem && golem instanceof RequesterGolemAccess access
							&& access.requestergolems$hasAssignmentTo(dimension, chestId)) {
						assigned.add(golem);
					}
				}
			}
			assigned.sort(Comparator.comparingInt(g -> ((RequesterGolemAccess) g).requestergolems$getPublicId()));
			List<String> lines = new ArrayList<>();
			for (int i = 0; i < Math.min(2, assigned.size()); i++) {
				RequesterGolemAccess access = (RequesterGolemAccess) assigned.get(i);
				lines.add(access.requestergolems$getDisplayName() + " (#"
						+ String.format(java.util.Locale.ROOT, "%06d", access.requestergolems$getPublicId()) + ")");
			}
			if (assigned.isEmpty()) lines.add("None");
			else if (assigned.size() > 2) lines.set(1, "… and " + (assigned.size() - 1) + " more");
			ServerPlayNetworking.send((net.minecraft.server.level.ServerPlayer) player,
					new RequesterAssignmentsListPayload(String.join("\n", lines)));
		});
	}
}

package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
	@Inject(method = "isTargetValidToPick", at = @At("HEAD"), cancellable = true)
	private static void requestergolems$rejectRequesterChest(
			PathfinderMob body,
			net.minecraft.world.level.Level level,
			net.minecraft.world.level.block.entity.BlockEntity blockEntity,
			java.util.Set<net.minecraft.core.GlobalPos> visitedPositions,
			java.util.Set<net.minecraft.core.GlobalPos> unreachablePositions,
			net.minecraft.world.phys.AABB targetBlockSearchArea,
			CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> cir
	) {
		if (blockEntity instanceof RequesterChestAccess requester
				&& requester.requestergolems$isRequester()) {
			cir.setReturnValue(null);
		}
	}

	@Inject(method = "checkExtraStartConditions", at = @At("HEAD"), cancellable = true)
	private void requestergolems$disableVanillaTransport(
			ServerLevel level,
			PathfinderMob body,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (body instanceof RequesterGolemAccess requester && requester.requestergolems$isRequester()) {
			cir.setReturnValue(false);
		}
	}
}

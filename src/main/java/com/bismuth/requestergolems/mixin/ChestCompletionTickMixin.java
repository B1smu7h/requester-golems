package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlock.class)
public abstract class ChestCompletionTickMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void requestergolems$endCompletionPulse(
			BlockState state,
			ServerLevel level,
			BlockPos pos,
			RandomSource random,
			CallbackInfo ci
	) {
		var blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof RequesterChestAccess requester
				&& requester.requestergolems$isCompletionPulseActive()) {
			requester.requestergolems$clearCompletionPulse();
			level.updateNeighborsAt(pos, state.getBlock());
		}
	}
}
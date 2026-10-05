package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Development conversion hook for Copper Chests.
 *
 * <p>ChestBlock inherits item interaction from BlockBehaviour in 26.3, so
 * this mixin targets the common interaction method and filters it down to
 * Copper Chests.</p>
 */
@Mixin(BlockBehaviour.class)
public abstract class ChestBlockMixin {
	@Inject(method = "neighborChanged", at = @At("TAIL"))
	private void requestergolems$detectRedstone(
			BlockState state,
			Level level,
			BlockPos pos,
			net.minecraft.world.level.block.Block block,
			Orientation orientation,
			boolean movedByPiston,
			CallbackInfo ci
	) {
		if (level.isClientSide() || !(state.getBlock() instanceof CopperChestBlock)) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof RequesterChestAccess requesterChest)
				|| !requesterChest.requestergolems$isRequester()) {
			return;
		}

		boolean powered = level.hasNeighborSignal(pos);
		boolean wasPowered = requesterChest.requestergolems$isRedstonePowered();
		if (powered != wasPowered) {
			requesterChest.requestergolems$setRedstonePowered(powered);
			if (powered) {
				requesterChest.requestergolems$activateRequests();
			}
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void requestergolems$convertWithDiamond(
			ItemStack itemStack,
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			InteractionHand hand,
			BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!(state.getBlock() instanceof CopperChestBlock)
				|| !itemStack.is(Items.DIAMOND)) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof RequesterChestAccess requesterChest)
				|| requesterChest.requestergolems$isRequester()) {
			return;
		}

		if (!level.isClientSide()) {
			requesterChest.requestergolems$setRequester(true);
			blockEntity.setChanged();
			itemStack.consume(1, player);
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}

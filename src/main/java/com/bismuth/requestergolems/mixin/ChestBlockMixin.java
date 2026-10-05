package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlockState;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Development conversion hook for Copper Chests.
 *
 * <p>The final conversion control will live inside the requester chest UI.
 * For now, a diamond used directly on a Copper Chest establishes the
 * persistent requester mode without introducing a second block.</p>
 */
@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {
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

			if (blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				chest.setCustomName(Component.literal("Requester Chest"));
				chest.setChanged();
			}

			itemStack.consume(1, player);
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}

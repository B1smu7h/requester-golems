package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestOpenMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void requestergolems$openRequesterChest(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!(level.getBlockEntity(pos) instanceof RequesterChestAccess access)
				|| !access.requestergolems$isRequester()) {
			return;
		}

		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			player.openMenu(new net.minecraft.world.SimpleMenuProvider(
					(containerId, inventory, ignoredPlayer) -> new RequesterChestMenu(containerId, inventory, chest),
					Component.translatable("container.requestergolems.requester_chest")
			));
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}

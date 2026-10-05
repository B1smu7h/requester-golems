package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds requester-mode metadata to vanilla chest block entities.
 *
 * <p>The temporary diamond conversion is a development checkpoint. The
 * production conversion control will move into the requester chest UI.</p>
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements RequesterChestAccess {
	private static final String REQUESTER_KEY = "requestergolems:requester";

	private boolean requestergolems$requester;

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);
	}

	@Inject(
			method = "startOpen",
			at = @At("HEAD"),
			cancellable = true
	)
	private void requestergolems$convertCopperChest(Player player, CallbackInfo ci) {
		if (this.requestergolems$requester
				|| player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
				|| !player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.DIAMOND)) {
			return;
		}

		ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
		if (!(chest.getBlockState().getBlock() instanceof CopperChestBlock)) {
			return;
		}

		if (!player.level().isClientSide()) {
			this.requestergolems$requester = true;
			player.getItemInHand(InteractionHand.MAIN_HAND).consume(1, player);
			chest.setCustomName(Component.literal("Requester Chest"));
			chest.setChanged();
		}

		ci.cancel();
	}
}

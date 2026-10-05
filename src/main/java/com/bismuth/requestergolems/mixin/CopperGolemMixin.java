package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterGolemAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the first piece of Requester Golems state to the vanilla Copper Golem.
 *
 * <p>For V1 development, a diamond converts a Copper Golem into requester
 * mode. The state is persisted with the entity so this is already a real
 * role rather than a temporary runtime flag.</p>
 */
@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin implements RequesterGolemAccess {
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

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void requestergolems$convertWithDiamond(
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (this.requestergolems$requester || player.getItemInHand(hand).isEmpty()
				|| !player.getItemInHand(hand).is(Items.DIAMOND)) {
			return;
		}

		if (!player.level().isClientSide()) {
			this.requestergolems$requester = true;
			player.getItemInHand(hand).consume(1, player);

			CopperGolem golem = (CopperGolem) (Object) this;
			golem.setCustomName(net.minecraft.network.chat.Component.literal("Requester Golem"));
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);
	}
}

package com.bismuth.requestergolems.mixin;

import java.util.UUID;

import com.bismuth.requestergolems.RequesterGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
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
	private static final String ROLLBACK_ACTIVE_KEY = "requestergolems:rollback_active";
	private static final String ROLLBACK_X_KEY = "requestergolems:rollback_x";
	private static final String ROLLBACK_Y_KEY = "requestergolems:rollback_y";
	private static final String ROLLBACK_Z_KEY = "requestergolems:rollback_z";
	private static final String RECOVERY_ACTIVE_KEY = "requestergolems:recovery_active";
	private static final String RECOVERY_JOB_ID_KEY = "requestergolems:recovery_job_id";
	private static final String RECOVERY_REQUESTER_X_KEY = "requestergolems:recovery_requester_x";
	private static final String RECOVERY_REQUESTER_Y_KEY = "requestergolems:recovery_requester_y";
	private static final String RECOVERY_REQUESTER_Z_KEY = "requestergolems:recovery_requester_z";

	private boolean requestergolems$requester;
	private BlockPos requestergolems$rollbackSource;
	private UUID requestergolems$recoveryJobId;
	private BlockPos requestergolems$recoveryRequesterPos;

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
	}

	@Override
	public BlockPos requestergolems$getRollbackSource() {
		return this.requestergolems$rollbackSource;
	}

	@Override
	public void requestergolems$setRollbackSource(BlockPos source) {
		this.requestergolems$rollbackSource = source == null ? null : source.immutable();
	}

	@Override
	public void requestergolems$clearRollbackSource() {
		this.requestergolems$rollbackSource = null;
	}

	@Override
	public UUID requestergolems$getRecoveryJobId() {
		return this.requestergolems$recoveryJobId;
	}

	@Override
	public BlockPos requestergolems$getRecoveryRequesterPos() {
		return this.requestergolems$recoveryRequesterPos;
	}

	@Override
	public void requestergolems$setRecoveryState(UUID jobId, BlockPos requesterPos) {
		this.requestergolems$recoveryJobId = jobId;
		this.requestergolems$recoveryRequesterPos = requesterPos == null ? null : requesterPos.immutable();
	}

	@Override
	public void requestergolems$clearRecoveryState() {
		this.requestergolems$recoveryJobId = null;
		this.requestergolems$recoveryRequesterPos = null;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void requestergolems$rollbackCarriedItem(CallbackInfo ci) {
		CopperGolem golem = (CopperGolem) (Object) this;
		if (golem.level().isClientSide() || this.requestergolems$rollbackSource == null) return;

		BlockPos sourcePos = this.requestergolems$rollbackSource;
		if (golem.getMainHandItem().isEmpty()) {
			this.requestergolems$rollbackSource = null;
			return;
		}

		double distanceSq = golem.position().distanceToSqr(sourcePos.getX() + 0.5, sourcePos.getY() + 0.5, sourcePos.getZ() + 0.5);
		if (distanceSq > 3.5 * 3.5) {
			golem.getNavigation().moveTo(sourcePos.getX() + 0.5, sourcePos.getY(), sourcePos.getZ() + 0.5, 1.0);
			return;
		}

		var state = golem.level().getBlockState(sourcePos);
		if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
			return;
		}

		Container source = ChestBlock.getContainer(
				chestBlock, state, golem.level(), sourcePos, false
		);
		if (source == null) return;

		ItemStack carried = golem.getMainHandItem();
		ItemStack remainder = carried.copy();
		for (int slot = 0; slot < source.getContainerSize() && !remainder.isEmpty(); slot++) {
			ItemStack existing = source.getItem(slot);
			if (existing.isEmpty() || !existing.is(remainder.getItem())) continue;
			int moved = Math.min(remainder.getCount(), source.getMaxStackSize());
			moved = Math.min(moved, existing.getMaxStackSize() - existing.getCount());
			if (moved > 0) {
				existing.grow(moved);
				remainder.shrink(moved);
				source.setChanged();
			}
		}
		for (int slot = 0; slot < source.getContainerSize() && !remainder.isEmpty(); slot++) {
			if (!source.getItem(slot).isEmpty() || !source.canPlaceItem(slot, remainder)) continue;
			int moved = Math.min(remainder.getCount(), source.getMaxStackSize());
			remainder.shrink(moved);
			source.setItem(slot, remainder.copyWithCount(moved));
		}

		golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, remainder);
		if (remainder.isEmpty()) {
			this.requestergolems$rollbackSource = null;
			golem.getNavigation().stop();
		}
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
		if (this.requestergolems$rollbackSource != null) {
			output.putBoolean(ROLLBACK_ACTIVE_KEY, true);
			output.putInt(ROLLBACK_X_KEY, this.requestergolems$rollbackSource.getX());
			output.putInt(ROLLBACK_Y_KEY, this.requestergolems$rollbackSource.getY());
			output.putInt(ROLLBACK_Z_KEY, this.requestergolems$rollbackSource.getZ());
		}
		if (this.requestergolems$recoveryJobId != null && this.requestergolems$recoveryRequesterPos != null) {
			output.putBoolean(RECOVERY_ACTIVE_KEY, true);
			output.putString(RECOVERY_JOB_ID_KEY, this.requestergolems$recoveryJobId.toString());
			output.putInt(RECOVERY_REQUESTER_X_KEY, this.requestergolems$recoveryRequesterPos.getX());
			output.putInt(RECOVERY_REQUESTER_Y_KEY, this.requestergolems$recoveryRequesterPos.getY());
			output.putInt(RECOVERY_REQUESTER_Z_KEY, this.requestergolems$recoveryRequesterPos.getZ());
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);
		if (input.getBooleanOr(ROLLBACK_ACTIVE_KEY, false)) {
			this.requestergolems$rollbackSource = new BlockPos(
					input.getIntOr(ROLLBACK_X_KEY, 0),
					input.getIntOr(ROLLBACK_Y_KEY, 0),
					input.getIntOr(ROLLBACK_Z_KEY, 0)
			);
		} else {
			this.requestergolems$rollbackSource = null;
		}

		this.requestergolems$recoveryJobId = null;
		this.requestergolems$recoveryRequesterPos = null;
		if (input.getBooleanOr(RECOVERY_ACTIVE_KEY, false)) {
			String jobId = input.getStringOr(RECOVERY_JOB_ID_KEY, "");
			try {
				this.requestergolems$recoveryJobId = UUID.fromString(jobId);
				this.requestergolems$recoveryRequesterPos = new BlockPos(
						input.getIntOr(RECOVERY_REQUESTER_X_KEY, 0),
						input.getIntOr(RECOVERY_REQUESTER_Y_KEY, 0),
						input.getIntOr(RECOVERY_REQUESTER_Z_KEY, 0)
				);
			} catch (IllegalArgumentException ignored) {
				this.requestergolems$recoveryJobId = null;
				this.requestergolems$recoveryRequesterPos = null;
			}
		}
	}
}

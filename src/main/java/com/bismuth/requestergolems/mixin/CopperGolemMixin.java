package com.bismuth.requestergolems.mixin;

import java.util.UUID;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import com.bismuth.requestergolems.RequesterGolemIdData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
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
 * <p>For V1.1, a redstone block converts a Copper Golem into requester
 * mode. The state is persisted with the entity so this is already a real
 * role rather than a temporary runtime flag.</p>
 */
@Mixin(CopperGolem.class)
public abstract class CopperGolemMixin implements RequesterGolemAccess {
	private static final String REQUESTER_KEY = "requestergolems:requester";
	private static final String PUBLIC_ID_KEY = "requestergolems:public_id";
	private static final String OWNER_UUID_KEY = "requestergolems:owner_uuid";
	private static final String DISPLAY_NAME_KEY = "requestergolems:display_name";
	private static final String ASSIGNMENTS_KEY = "requestergolems:assignments";
	private final java.util.Set<String> requestergolems$assignments = new java.util.HashSet<>();
	private static final String ROLLBACK_ACTIVE_KEY = "requestergolems:rollback_active";
	private static final String ROLLBACK_X_KEY = "requestergolems:rollback_x";
	private static final String ROLLBACK_Y_KEY = "requestergolems:rollback_y";
	private static final String ROLLBACK_Z_KEY = "requestergolems:rollback_z";
	private static final String RECOVERY_ACTIVE_KEY = "requestergolems:recovery_active";
	private static final String RECOVERY_JOB_ID_KEY = "requestergolems:recovery_job_id";
	private static final String RECOVERY_REQUESTER_X_KEY = "requestergolems:recovery_requester_x";
	private static final String RECOVERY_REQUESTER_Y_KEY = "requestergolems:recovery_requester_y";
	private static final String RECOVERY_REQUESTER_Z_KEY = "requestergolems:recovery_requester_z";
	private static final String RECOVERY_SOURCE_X_KEY = "requestergolems:recovery_source_x";
	private static final String RECOVERY_SOURCE_Y_KEY = "requestergolems:recovery_source_y";
	private static final String RECOVERY_SOURCE_Z_KEY = "requestergolems:recovery_source_z";

	private boolean requestergolems$requester;
	private int requestergolems$publicId;
	private UUID requestergolems$owner;
	private String requestergolems$displayName = "Requester Golem";
	private BlockPos requestergolems$rollbackSource;
	private UUID requestergolems$recoveryJobId;
	private BlockPos requestergolems$recoveryRequesterPos;
	private BlockPos requestergolems$recoverySourcePos;

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
	}

	@Override
	public int requestergolems$getPublicId() {
		return this.requestergolems$publicId;
	}

	@Override
	public void requestergolems$setPublicId(int id) {
		this.requestergolems$publicId = Math.max(0, id);
	}

	@Override
	public UUID requestergolems$getOwner() {
		return this.requestergolems$owner;
	}

	@Override
	public void requestergolems$setOwner(UUID owner) {
		this.requestergolems$owner = owner;
	}

	@Override
	public boolean requestergolems$claimIfUnowned(UUID playerId) {
		if (!this.requestergolems$requester || playerId == null || this.requestergolems$owner != null) return false;
		this.requestergolems$owner = playerId;
		return true;
	}

	@Override
	public boolean requestergolems$isAssignedTo(String dimensionId, UUID requesterChestId) {
		return this.requestergolems$assignments.isEmpty()
				|| this.requestergolems$assignments.contains(dimensionId + "|" + requesterChestId);
	}

	@Override
	public void requestergolems$setAssignedTo(String dimensionId, UUID requesterChestId, boolean assigned) {
		if (dimensionId == null || requesterChestId == null || dimensionId.contains("|")) return;
		String key = dimensionId + "|" + requesterChestId;
		if (assigned) this.requestergolems$assignments.add(key);
		else this.requestergolems$assignments.remove(key);
	}

	@Override
	public String requestergolems$getDisplayName() {
		return this.requestergolems$displayName;
	}

	@Override
	public void requestergolems$setDisplayName(String name) {
		String cleaned = name == null ? "" : name.strip().replaceAll("[\\p{Cntrl}]", "");
		if (cleaned.isEmpty()) cleaned = "Requester Golem";
		if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32);
		this.requestergolems$displayName = cleaned;
		((CopperGolem) (Object) this).setCustomName(net.minecraft.network.chat.Component.literal(cleaned));
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
	public BlockPos requestergolems$getRecoverySourcePos() {
		return this.requestergolems$recoverySourcePos;
	}

	@Override
	public void requestergolems$setRecoveryState(UUID jobId, BlockPos requesterPos, BlockPos sourcePos) {
		this.requestergolems$recoveryJobId = jobId;
		this.requestergolems$recoveryRequesterPos = requesterPos == null ? null : requesterPos.immutable();
		this.requestergolems$recoverySourcePos = sourcePos == null ? null : sourcePos.immutable();
	}

	@Override
	public void requestergolems$clearRecoveryState() {
		this.requestergolems$recoveryJobId = null;
		this.requestergolems$recoveryRequesterPos = null;
		this.requestergolems$recoverySourcePos = null;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void requestergolems$rollbackCarriedItem(CallbackInfo ci) {
		CopperGolem golem = (CopperGolem) (Object) this;
		if (golem.level() instanceof ServerLevel serverLevel && this.requestergolems$requester) {
			if (this.requestergolems$publicId <= 0) {
				this.requestergolems$publicId = RequesterGolemIdData.allocate(serverLevel);
			}
			if (this.requestergolems$displayName == null || this.requestergolems$displayName.isBlank()) {
				this.requestergolems$setDisplayName("Requester Golem");
			}
		}
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

	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void requestergolems$dropCarriedItemOnDeath(
			ServerLevel level,
			CallbackInfo ci
	) {
		if (!this.requestergolems$requester) return;

		CopperGolem golem = (CopperGolem) (Object) this;
		ItemStack carried = golem.getItemBySlot(EquipmentSlot.MAINHAND);

		// Requester transport uses the vanilla main hand as its cargo slot. Make
		// that cargo an explicit death drop before Copper Golem's normal equipment
		// drop hook runs, then clear the slot so vanilla cannot drop it twice.
		if (!carried.isEmpty()) {
			golem.spawnAtLocation(level, carried.copy());
			golem.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}

		// The transport job is separate from the physical stack. Recover it even
		// if the stack was already removed from the golem before death.
		UUID recoveryJobId = this.requestergolems$recoveryJobId;
		BlockPos recoveryRequesterPos = this.requestergolems$recoveryRequesterPos;
		if (recoveryJobId != null && recoveryRequesterPos != null) {
			level.getChunkAt(recoveryRequesterPos);
			if (level.getBlockEntity(recoveryRequesterPos) instanceof RequesterChestAccess requesterChest) {
				requesterChest.requestergolems$recoverJob(recoveryJobId);
			}
		}
		this.requestergolems$clearRecoveryState();
		this.requestergolems$clearRollbackSource();
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void requestergolems$claimLegacyGolem(
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (!this.requestergolems$requester || hand != InteractionHand.MAIN_HAND
				|| !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
			return;
		}
		if (player.level().isClientSide()) {
			cir.setReturnValue(InteractionResult.SUCCESS);
			return;
		}
		if (this.requestergolems$owner == null) {
			if (this.requestergolems$claimIfUnowned(player.getUUID())) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal(
						"Claimed " + this.requestergolems$displayName + " (#"
								+ String.format(java.util.Locale.ROOT, "%06d", this.requestergolems$publicId) + ")."
				), true);
			} else {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("Could not claim this Requester Golem."), true);
			}
		} else if (this.requestergolems$owner.equals(player.getUUID())) {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("You already own this Requester Golem."), true);
		} else {
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("This Requester Golem belongs to another player."), true);
		}
		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void requestergolems$convertWithRedstoneBlock(
			Player player,
			InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (this.requestergolems$requester || player.getItemInHand(hand).isEmpty()
				|| !player.getItemInHand(hand).is(Items.REDSTONE_BLOCK)) {
			return;
		}

		if (!player.level().isClientSide()) {
			this.requestergolems$requester = true;
			CopperGolem golem = (CopperGolem) (Object) this;
			if (golem.level() instanceof ServerLevel serverLevel) {
				this.requestergolems$publicId = RequesterGolemIdData.allocate(serverLevel);
			}
			this.requestergolems$owner = player.getUUID();
			this.requestergolems$setDisplayName("Requester Golem");
			player.getItemInHand(hand).consume(1, player);
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);
		output.putInt(PUBLIC_ID_KEY, this.requestergolems$publicId);
		output.putString(DISPLAY_NAME_KEY, this.requestergolems$displayName == null ? "Requester Golem" : this.requestergolems$displayName);
		output.putString(ASSIGNMENTS_KEY, String.join(";", this.requestergolems$assignments));
		if (this.requestergolems$owner != null) {
			output.putString(OWNER_UUID_KEY, this.requestergolems$owner.toString());
		}
		if (this.requestergolems$rollbackSource != null) {
			output.putBoolean(ROLLBACK_ACTIVE_KEY, true);
			output.putInt(ROLLBACK_X_KEY, this.requestergolems$rollbackSource.getX());
			output.putInt(ROLLBACK_Y_KEY, this.requestergolems$rollbackSource.getY());
			output.putInt(ROLLBACK_Z_KEY, this.requestergolems$rollbackSource.getZ());
		}
		if (this.requestergolems$recoveryJobId != null && this.requestergolems$recoveryRequesterPos != null && this.requestergolems$recoverySourcePos != null) {
			output.putBoolean(RECOVERY_ACTIVE_KEY, true);
			output.putString(RECOVERY_JOB_ID_KEY, this.requestergolems$recoveryJobId.toString());
			output.putInt(RECOVERY_REQUESTER_X_KEY, this.requestergolems$recoveryRequesterPos.getX());
			output.putInt(RECOVERY_REQUESTER_Y_KEY, this.requestergolems$recoveryRequesterPos.getY());
			output.putInt(RECOVERY_REQUESTER_Z_KEY, this.requestergolems$recoveryRequesterPos.getZ());
			output.putInt(RECOVERY_SOURCE_X_KEY, this.requestergolems$recoverySourcePos.getX());
			output.putInt(RECOVERY_SOURCE_Y_KEY, this.requestergolems$recoverySourcePos.getY());
			output.putInt(RECOVERY_SOURCE_Z_KEY, this.requestergolems$recoverySourcePos.getZ());
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);
		this.requestergolems$publicId = Math.max(0, input.getIntOr(PUBLIC_ID_KEY, 0));
		this.requestergolems$displayName = input.getStringOr(DISPLAY_NAME_KEY, "Requester Golem");
		this.requestergolems$assignments.clear();
		String assignments = input.getStringOr(ASSIGNMENTS_KEY, "");
		for (String assignment : assignments.split(";")) {
			if (assignment.contains("|") && assignment.length() <= 256) this.requestergolems$assignments.add(assignment);
		}
		String ownerString = input.getStringOr(OWNER_UUID_KEY, "");
		try {
			this.requestergolems$owner = ownerString.isBlank() ? null : UUID.fromString(ownerString);
		} catch (IllegalArgumentException ignored) {
			this.requestergolems$owner = null;
		}
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
				this.requestergolems$recoverySourcePos = new BlockPos(
						input.getIntOr(RECOVERY_SOURCE_X_KEY, 0),
						input.getIntOr(RECOVERY_SOURCE_Y_KEY, 0),
						input.getIntOr(RECOVERY_SOURCE_Z_KEY, 0)
				);
				this.requestergolems$rollbackSource = this.requestergolems$recoverySourcePos;
			} catch (IllegalArgumentException ignored) {
				this.requestergolems$recoveryJobId = null;
				this.requestergolems$recoveryRequesterPos = null;
				this.requestergolems$recoverySourcePos = null;
			}
		}
		if (this.requestergolems$requester) {
			this.requestergolems$setDisplayName(this.requestergolems$displayName);
		}
	}
}

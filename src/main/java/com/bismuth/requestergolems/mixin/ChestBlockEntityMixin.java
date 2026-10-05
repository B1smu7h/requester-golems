package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterJob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.NonNullList;

/**
 * Adds requester-mode metadata and ten persistent request slots to vanilla
 * chest block entities.
 *
 * <p>The underlying 27-slot chest inventory remains untouched. Request slots
 * are a separate data model and will later be presented by the requester
 * chest menu.</p>
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements RequesterChestAccess {
	private static final String REQUESTER_KEY = "requestergolems:requester";
	private static final String REQUEST_KEY_PREFIX = "requestergolems:request_";
	private static final String ACTIVE_JOBS_KEY = "requestergolems:active_jobs";
	private static final String ACTIVE_JOB_COUNT_KEY = "requestergolems:active_job_count";
	private static final String ACTIVE_JOB_ID_PREFIX = "requestergolems:active_job_id_";
	private static final String ACTIVE_JOB_ITEM_PREFIX = "requestergolems:active_job_item_";
	private static final String REDSTONE_KEY = "requestergolems:redstone_powered";

	private boolean requestergolems$requester;
	private boolean requestergolems$redstonePowered;
private boolean requestergolems$completionPulse;
	private List<RequesterJob> requestergolems$activeJobs;
	private NonNullList<ItemStack> requestergolems$requests;

	private List<RequesterJob> requestergolems$activeJobs() {
		if (this.requestergolems$activeJobs == null) {
			this.requestergolems$activeJobs = new ArrayList<>();
		}
		return this.requestergolems$activeJobs;
	}

	private NonNullList<ItemStack> requestergolems$requests() {
		if (this.requestergolems$requests == null) {
			this.requestergolems$requests =
					NonNullList.withSize(RequesterChestAccess.REQUEST_SLOT_COUNT, ItemStack.EMPTY);
		}
		return this.requestergolems$requests;
	}

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
	}

	@Override
	public boolean requestergolems$isRedstonePowered() {
		return this.requestergolems$redstonePowered;
	}

	@Override
	public void requestergolems$setRedstonePowered(boolean powered) {
		this.requestergolems$redstonePowered = powered;
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public void requestergolems$emitCompletionPulse() {
		if (!this.requestergolems$requester) return;

		// Treat our own output as a temporary powered state so the signal
		// returning through redstone cannot look like a fresh rising edge.
		this.requestergolems$redstonePowered = true;
		this.requestergolems$completionPulse = true;
		ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
		if (chest.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
			level.scheduleTick(chest.getBlockPos(), level.getBlockState(chest.getBlockPos()).getBlock(), 2);
			level.updateNeighborsAt(chest.getBlockPos(), level.getBlockState(chest.getBlockPos()).getBlock());
		}
		chest.setChanged();
	}

	@Override
	public boolean requestergolems$isCompletionPulseActive() {
		return this.requestergolems$completionPulse;
	}

	@Override
	public void requestergolems$clearCompletionPulse() {
		this.requestergolems$completionPulse = false;
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public void requestergolems$activateRequests() {
		if (!this.requestergolems$requester) return;
		for (ItemStack request : this.requestergolems$requests()) {
			if (request.isEmpty()) continue;
			int remaining = request.getCount();
			while (remaining > 0) {
				int amount = Math.min(16, remaining);
				this.requestergolems$activeJobs().add(
						RequesterJob.create(request.copyWithCount(amount))
				);
				remaining -= amount;
			}
		}
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public boolean requestergolems$hasActiveJobs() {
		return this.requestergolems$activeJobs().stream()
				.anyMatch(job -> !job.isComplete());
	}

	@Override
	public RequesterJob requestergolems$claimJob() {
		for (RequesterJob job : this.requestergolems$activeJobs()) {
			if (job.state() == RequesterJob.State.WAITING && !job.isComplete()) {
				job.setState(RequesterJob.State.IN_PROGRESS);
				((ChestBlockEntity) (Object) this).setChanged();
				return job;
			}
		}
		return null;
	}

	@Override
	public void requestergolems$returnJob(RequesterJob job) {
		if (job == null || job.isComplete()) return;
		job.setState(RequesterJob.State.WAITING);
		if (!this.requestergolems$activeJobs().contains(job)) {
			this.requestergolems$activeJobs().add(0, job);
		}
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public boolean requestergolems$isJobActive(UUID jobId) {
		return this.requestergolems$activeJobs().stream()
				.anyMatch(job -> job.id().equals(jobId) && !job.isComplete());
	}

	@Override
	public void requestergolems$completeJob(UUID jobId) {
		this.requestergolems$activeJobs().removeIf(job -> job.id().equals(jobId));
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public boolean requestergolems$cancelJob(UUID jobId) {
		boolean removed = this.requestergolems$activeJobs().removeIf(job -> job.id().equals(jobId));
		if (removed) {
			((ChestBlockEntity) (Object) this).setChanged();
		}
		return removed;
	}

	@Override
	public ItemStack requestergolems$getRequest(int slot) {
		if (slot < 0 || slot >= RequesterChestAccess.REQUEST_SLOT_COUNT) {
			throw new IndexOutOfBoundsException("Invalid requester slot: " + slot);
		}
		return this.requestergolems$requests().get(slot);
	}

	@Override
	public void requestergolems$setRequest(int slot, ItemStack stack) {
		if (slot < 0 || slot >= RequesterChestAccess.REQUEST_SLOT_COUNT) {
			throw new IndexOutOfBoundsException("Invalid requester slot: " + slot);
		}

		this.requestergolems$requests().set(slot, stack.copy());
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void requestergolems$createRequesterMenu(
			int containerId,
			Inventory inventory,
			CallbackInfoReturnable<AbstractContainerMenu> cir
	) {
		if (this.requestergolems$requester) {
			cir.setReturnValue(new RequesterChestMenu(containerId, inventory, (ChestBlockEntity) (Object) this));
		}
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);
		output.putBoolean(REDSTONE_KEY, this.requestergolems$redstonePowered);
		// Completion pulses are transient and intentionally are not persisted.
		output.putInt(ACTIVE_JOB_COUNT_KEY, this.requestergolems$activeJobs().size());
		for (int index = 0; index < this.requestergolems$activeJobs().size(); index++) {
			RequesterJob job = this.requestergolems$activeJobs().get(index);
			output.putString(ACTIVE_JOB_ID_PREFIX + index, job.id().toString());
			output.store(ACTIVE_JOB_ITEM_PREFIX + index, ItemStack.CODEC, job.stack());
		}

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			ItemStack request = this.requestergolems$requests().get(slot);
			if (!request.isEmpty()) {
				output.store(REQUEST_KEY_PREFIX + slot, ItemStack.CODEC, request);
			}
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);
		this.requestergolems$redstonePowered = input.getBooleanOr(REDSTONE_KEY, false);
		this.requestergolems$completionPulse = false;
		this.requestergolems$activeJobs().clear();
		int jobCount = input.getIntOr(ACTIVE_JOB_COUNT_KEY, 0);
		for (int index = 0; index < jobCount; index++) {
			String idString = input.getStringOr(ACTIVE_JOB_ID_PREFIX + index, "");
			if (idString.isEmpty()) continue;

			ItemStack stack = input.read(ACTIVE_JOB_ITEM_PREFIX + index, ItemStack.CODEC).orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) continue;

			try {
				// A job that was in progress when the chunk was saved has no
				// surviving worker reference, so it safely returns to WAITING.
				this.requestergolems$activeJobs().add(
						new RequesterJob(UUID.fromString(idString), stack, RequesterJob.State.WAITING)
				);
			} catch (IllegalArgumentException ignored) {
				// Ignore malformed job IDs rather than making the whole chest fail to load.
			}
		}

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			this.requestergolems$requests().set(
					slot,
					input.read(REQUEST_KEY_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY)
			);
		}
	}
}

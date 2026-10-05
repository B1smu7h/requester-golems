package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterJob;
import com.bismuth.requestergolems.RequesterRequest;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds requester-mode metadata, request slots, high-level requests, and
 * internal transport jobs to vanilla chest block entities.
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements RequesterChestAccess {
	private static final String REQUESTER_KEY = "requestergolems:requester";
	private static final String REQUEST_KEY_PREFIX = "requestergolems:request_";

	private static final String ACTIVE_REQUEST_COUNT_KEY = "requestergolems:active_request_count";
	private static final String ACTIVE_REQUEST_ID_PREFIX = "requestergolems:active_request_id_";
	private static final String ACTIVE_REQUEST_ITEM_PREFIX = "requestergolems:active_request_item_";
	private static final String ACTIVE_REQUEST_ORIGINAL_COUNT_PREFIX = "requestergolems:active_request_original_count_";
	private static final String ACTIVE_REQUEST_REMAINING_COUNT_PREFIX = "requestergolems:active_request_remaining_count_";
	private static final String ACTIVE_REQUEST_CREATED_AT_PREFIX = "requestergolems:active_request_created_at_";

	private static final String ACTIVE_JOB_COUNT_KEY = "requestergolems:active_job_count";
	private static final String ACTIVE_JOB_ID_PREFIX = "requestergolems:active_job_id_";
	private static final String ACTIVE_JOB_REQUEST_ID_PREFIX = "requestergolems:active_job_request_id_";
	private static final String ACTIVE_JOB_ITEM_PREFIX = "requestergolems:active_job_item_";
	private static final String ACTIVE_JOB_ORIGINAL_COUNT_PREFIX = "requestergolems:active_job_original_count_";

	private static final String REDSTONE_KEY = "requestergolems:redstone_powered";

	private boolean requestergolems$requester;
	private boolean requestergolems$redstonePowered;
	private boolean requestergolems$completionPulse;
	private List<RequesterRequest> requestergolems$activeRequests;
	private List<RequesterJob> requestergolems$activeJobs;
	private NonNullList<ItemStack> requestergolems$requests;

	private List<RequesterRequest> requestergolems$activeRequests() {
		if (this.requestergolems$activeRequests == null) {
			this.requestergolems$activeRequests = new ArrayList<>();
		}
		return this.requestergolems$activeRequests;
	}

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

	private ChestBlockEntity requestergolems$chest() {
		return (ChestBlockEntity) (Object) this;
	}

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
		this.requestergolems$chest().setChanged();
	}

	@Override
	public boolean requestergolems$isRedstonePowered() {
		return this.requestergolems$redstonePowered;
	}

	@Override
	public void requestergolems$setRedstonePowered(boolean powered) {
		this.requestergolems$redstonePowered = powered;
		this.requestergolems$chest().setChanged();
	}

	@Override
	public void requestergolems$emitCompletionPulse() {
		if (!this.requestergolems$requester) return;

		this.requestergolems$redstonePowered = true;
		this.requestergolems$completionPulse = true;
		ChestBlockEntity chest = this.requestergolems$chest();
		if (chest.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
			level.scheduleTick(
					chest.getBlockPos(),
					level.getBlockState(chest.getBlockPos()).getBlock(),
					2
			);
			level.updateNeighborsAt(
					chest.getBlockPos(),
					level.getBlockState(chest.getBlockPos()).getBlock()
			);
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
		this.requestergolems$chest().setChanged();
	}

	@Override
	public void requestergolems$activateRequests() {
		if (!this.requestergolems$requester) return;

		long createdAt = this.requestergolems$chest().getLevel() != null
				? this.requestergolems$chest().getLevel().getGameTime()
				: 0L;

		for (ItemStack request : this.requestergolems$requests()) {
			if (!request.isEmpty()) {
				this.requestergolems$activeRequests().add(
						RequesterRequest.create(request, createdAt)
				);
			}
		}

		this.requestergolems$chest().setChanged();
	}

	@Override
	public boolean requestergolems$hasActiveRequests() {
		return this.requestergolems$activeRequests().stream()
				.anyMatch(request -> !request.isComplete());
	}

	@Override
	public List<RequesterRequest> requestergolems$getActiveRequests() {
		return this.requestergolems$activeRequests().stream()
				.filter(request -> !request.isComplete())
				.map(RequesterRequest::copy)
				.toList();
	}

	@Override
	public boolean requestergolems$isRequestActive(UUID requestId) {
		return this.requestergolems$activeRequests().stream()
				.anyMatch(request -> request.id().equals(requestId) && !request.isComplete());
	}

	@Override
	public boolean requestergolems$cancelRequest(UUID requestId) {
		boolean removed = this.requestergolems$activeRequests().removeIf(
				request -> request.id().equals(requestId)
		);
		if (removed) {
			this.requestergolems$activeJobs().removeIf(
					job -> job.requestId().equals(requestId)
			);
			this.requestergolems$chest().setChanged();
		}
		return removed;
	}

	@Override
	public void requestergolems$deliverToRequest(UUID requestId, int amount) {
		if (amount <= 0) return;

		for (RequesterRequest request : this.requestergolems$activeRequests()) {
			if (!request.id().equals(requestId) || request.isComplete()) continue;
			request.deliver(amount);
			this.requestergolems$chest().setChanged();
			return;
		}
	}

	@Override
	public boolean requestergolems$hasActiveJobs() {
		return !this.requestergolems$activeJobs().isEmpty();
	}

	@Override
	public List<RequesterJob> requestergolems$getActiveJobs() {
		return this.requestergolems$activeJobs().stream()
				.map(RequesterJob::copy)
				.toList();
	}

	@Override
	public RequesterJob requestergolems$claimJob() {
		for (RequesterJob job : this.requestergolems$activeJobs()) {
			if (job.state() == RequesterJob.State.WAITING && !job.isComplete()) {
				job.setState(RequesterJob.State.IN_PROGRESS);
				this.requestergolems$chest().setChanged();
				return job;
			}
		}

		for (RequesterRequest request : this.requestergolems$activeRequests()) {
			if (request.isComplete()) continue;

			int reserved = this.requestergolems$activeJobs().stream()
					.filter(job -> job.requestId().equals(request.id()) && !job.isComplete())
					.mapToInt(job -> job.stack().getCount())
					.sum();

			int availableToReserve = request.remainingCount() - reserved;
			if (availableToReserve <= 0) continue;

			int amount = Math.min(16, availableToReserve);
			RequesterJob job = RequesterJob.create(
					request.id(),
					request.requestedItem().copyWithCount(amount)
			);
			job.setState(RequesterJob.State.IN_PROGRESS);
			this.requestergolems$activeJobs().add(job);
			this.requestergolems$chest().setChanged();
			return job;
		}

		return null;
	}

	@Override
	public void requestergolems$returnJob(RequesterJob job) {
		if (job == null || job.isComplete()) return;

		job.setState(RequesterJob.State.WAITING);
		if (this.requestergolems$activeJobs().stream().noneMatch(
				existing -> existing.id().equals(job.id())
		)) {
			this.requestergolems$activeJobs().add(0, job);
		}
		this.requestergolems$chest().setChanged();
	}

	@Override
	public boolean requestergolems$isJobActive(UUID jobId) {
		return this.requestergolems$activeJobs().stream()
				.anyMatch(job ->
						job.id().equals(jobId)
								&& !job.isComplete()
								&& this.requestergolems$isRequestActive(job.requestId())
				);
	}

	@Override
	public void requestergolems$completeJob(UUID jobId) {
		RequesterJob completed = null;
		for (RequesterJob job : this.requestergolems$activeJobs()) {
			if (job.id().equals(jobId)) {
				completed = job;
				break;
			}
		}

		if (completed == null) return;

		this.requestergolems$activeJobs().removeIf(job -> job.id().equals(jobId));

		boolean requestCompleted = this.requestergolems$activeRequests().stream()
				.anyMatch(request ->
						request.id().equals(completed.requestId()) && request.isComplete()
				);

		if (requestCompleted) {
			this.requestergolems$activeRequests().removeIf(
					request -> request.id().equals(completed.requestId())
			);
			if (!this.requestergolems$hasActiveRequests()) {
				this.requestergolems$emitCompletionPulse();
			}
		}

		this.requestergolems$chest().setChanged();
	}

	@Override
	public boolean requestergolems$cancelJob(UUID jobId) {
		boolean removed = this.requestergolems$activeJobs().removeIf(job -> job.id().equals(jobId));
		if (removed) {
			this.requestergolems$chest().setChanged();
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
		this.requestergolems$chest().setChanged();
	}

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void requestergolems$createRequesterMenu(
			int containerId,
			Inventory inventory,
			CallbackInfoReturnable<AbstractContainerMenu> cir
	) {
		if (this.requestergolems$requester) {
			cir.setReturnValue(
					new RequesterChestMenu(
							containerId,
							inventory,
							this.requestergolems$chest()
					)
			);
		}
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);
		output.putBoolean(REDSTONE_KEY, this.requestergolems$redstonePowered);

		List<RequesterRequest> requests = this.requestergolems$activeRequests();
		output.putInt(ACTIVE_REQUEST_COUNT_KEY, requests.size());
		for (int index = 0; index < requests.size(); index++) {
			RequesterRequest request = requests.get(index);
			output.putString(ACTIVE_REQUEST_ID_PREFIX + index, request.id().toString());
			output.store(
					ACTIVE_REQUEST_ITEM_PREFIX + index,
					ItemStack.CODEC,
					request.requestedItem()
			);
			output.putInt(
					ACTIVE_REQUEST_ORIGINAL_COUNT_PREFIX + index,
					request.originalCount()
			);
			output.putInt(
					ACTIVE_REQUEST_REMAINING_COUNT_PREFIX + index,
					request.remainingCount()
			);
			output.putLong(
					ACTIVE_REQUEST_CREATED_AT_PREFIX + index,
					request.createdAt()
			);
		}

		List<RequesterJob> jobs = this.requestergolems$activeJobs();
		output.putInt(ACTIVE_JOB_COUNT_KEY, jobs.size());
		for (int index = 0; index < jobs.size(); index++) {
			RequesterJob job = jobs.get(index);
			output.putString(ACTIVE_JOB_ID_PREFIX + index, job.id().toString());
			output.putString(ACTIVE_JOB_REQUEST_ID_PREFIX + index, job.requestId().toString());
			output.store(ACTIVE_JOB_ITEM_PREFIX + index, ItemStack.CODEC, job.stack());
			output.putInt(
					ACTIVE_JOB_ORIGINAL_COUNT_PREFIX + index,
					job.originalCount()
			);
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
		this.requestergolems$activeRequests().clear();
		this.requestergolems$activeJobs().clear();

		int requestCount = input.getIntOr(ACTIVE_REQUEST_COUNT_KEY, 0);
		for (int index = 0; index < requestCount; index++) {
			String idString = input.getStringOr(ACTIVE_REQUEST_ID_PREFIX + index, "");
			if (idString.isEmpty()) continue;

			ItemStack item = input.read(
					ACTIVE_REQUEST_ITEM_PREFIX + index,
					ItemStack.CODEC
			).orElse(ItemStack.EMPTY);
			if (item.isEmpty()) continue;

			try {
				int original = input.getIntOr(
						ACTIVE_REQUEST_ORIGINAL_COUNT_PREFIX + index,
						item.getCount()
				);
				int remaining = input.getIntOr(
						ACTIVE_REQUEST_REMAINING_COUNT_PREFIX + index,
						original
				);
				long createdAt = input.getLongOr(
						ACTIVE_REQUEST_CREATED_AT_PREFIX + index,
						0L
				);
				RequesterRequest request = new RequesterRequest(
						UUID.fromString(idString),
						item,
						original,
						remaining,
						createdAt
				);
				if (!request.isComplete()) {
					this.requestergolems$activeRequests().add(request);
				}
			} catch (IllegalArgumentException ignored) {
				// Ignore malformed request IDs rather than failing the chest load.
			}
		}

		int jobCount = input.getIntOr(ACTIVE_JOB_COUNT_KEY, 0);
		for (int index = 0; index < jobCount; index++) {
			String idString = input.getStringOr(ACTIVE_JOB_ID_PREFIX + index, "");
			String requestIdString = input.getStringOr(
					ACTIVE_JOB_REQUEST_ID_PREFIX + index,
					""
			);
			if (idString.isEmpty() || requestIdString.isEmpty()) continue;

			ItemStack stack = input.read(
					ACTIVE_JOB_ITEM_PREFIX + index,
					ItemStack.CODEC
			).orElse(ItemStack.EMPTY);
			if (stack.isEmpty()) continue;

			try {
				UUID jobId = UUID.fromString(idString);
				UUID requestId = UUID.fromString(requestIdString);
				if (!this.requestergolems$isRequestActive(requestId)) continue;

				this.requestergolems$activeJobs().add(
						new RequesterJob(
								jobId,
								requestId,
								stack,
								input.getIntOr(
										ACTIVE_JOB_ORIGINAL_COUNT_PREFIX + index,
										stack.getCount()
								),
								RequesterJob.State.WAITING
						)
				);
			} catch (IllegalArgumentException ignored) {
				// Ignore malformed job IDs rather than failing the chest load.
			}
		}

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			this.requestergolems$requests().set(
					slot,
					input.read(
							REQUEST_KEY_PREFIX + slot,
							ItemStack.CODEC
					).orElse(ItemStack.EMPTY)
			);
		}
	}
}

package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;
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
	private static final String REDSTONE_KEY = "requestergolems:redstone_powered";

	private boolean requestergolems$requester;
	private boolean requestergolems$redstonePowered;
private boolean requestergolems$completionPulse;
	private List<ItemStack> requestergolems$activeJobs;
	private NonNullList<ItemStack> requestergolems$requests;

	private List<ItemStack> requestergolems$activeJobs() {
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
	public void requestergolems$activateRequests() {
		if (!this.requestergolems$requester) return;
		for (ItemStack request : this.requestergolems$requests()) {
			if (request.isEmpty()) continue;
			int remaining = request.getCount();
			while (remaining > 0) {
				int amount = Math.min(16, remaining);
				this.requestergolems$activeJobs().add(request.copyWithCount(amount));
				remaining -= amount;
			}
		}
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Override
	public boolean requestergolems$hasActiveJobs() {
		return !this.requestergolems$activeJobs().isEmpty();
	}

	@Override
	public ItemStack requestergolems$claimJob() {
		if (this.requestergolems$activeJobs().isEmpty()) return ItemStack.EMPTY;
		ItemStack job = this.requestergolems$activeJobs().remove(0);
		((ChestBlockEntity) (Object) this).setChanged();
		return job;
	}

	@Override
	public void requestergolems$returnJob(ItemStack job) {
		if (!job.isEmpty()) {
			this.requestergolems$activeJobs().add(0, job.copy());
			((ChestBlockEntity) (Object) this).setChanged();
		}
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

	@Inject(method = "tick", at = @At("HEAD"))
	private void requestergolems$tickCompletionPulse(
			CallbackInfo ci
	) {
		if (!this.requestergolems$completionPulse) return;
		this.requestergolems$completionPulse = false;
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
		ValueOutput.TypedOutputList<ItemStack> jobs = output.list(ACTIVE_JOBS_KEY, ItemStack.CODEC);
		for (ItemStack job : this.requestergolems$activeJobs()) jobs.add(job);

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
		this.requestergolems$activeJobs().addAll(input.listOrEmpty(ACTIVE_JOBS_KEY, ItemStack.CODEC).stream().map(ItemStack::copy).toList());

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			this.requestergolems$requests().set(
					slot,
					input.read(REQUEST_KEY_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY)
			);
		}
	}
}

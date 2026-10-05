package com.bismuth.requestergolems;

import net.minecraft.world.item.ItemStack;

/**
 * Persistent role and request-slot state layered onto a vanilla chest.
 */
public interface RequesterChestAccess {
	int REQUEST_SLOT_COUNT = 10;

	boolean requestergolems$isRequester();

	void requestergolems$setRequester(boolean requester);

	boolean requestergolems$isRedstonePowered();

	void requestergolems$setRedstonePowered(boolean powered);

	void requestergolems$emitCompletionPulse();

	boolean requestergolems$isCompletionPulseActive();

	void requestergolems$activateRequests();

	boolean requestergolems$hasActiveJobs();

	ItemStack requestergolems$claimJob();

	void requestergolems$returnJob(ItemStack job);

	ItemStack requestergolems$getRequest(int slot);

	void requestergolems$setRequest(int slot, ItemStack stack);
}
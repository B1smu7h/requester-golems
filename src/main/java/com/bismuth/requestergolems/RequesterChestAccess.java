package com.bismuth.requestergolems;

import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * Persistent role, request-slot, request, and transport state layered onto a vanilla chest.
 */
public interface RequesterChestAccess {
	int REQUEST_SLOT_COUNT = 9;

	boolean requestergolems$isRequester();
	void requestergolems$setRequester(boolean requester);

	boolean requestergolems$isRedstonePowered();
	void requestergolems$setRedstonePowered(boolean powered);

	void requestergolems$emitCompletionPulse();
	boolean requestergolems$isCompletionPulseActive();
	void requestergolems$clearCompletionPulse();

	/** Activates each non-empty request slot as a separate high-level request. */
	void requestergolems$activateRequests();

	boolean requestergolems$hasActiveRequests();
	List<RequesterRequest> requestergolems$getActiveRequests();
	boolean requestergolems$isRequestActive(UUID requestId);
	boolean requestergolems$isRequestCancelling(UUID requestId);
	void requestergolems$finalizeCancelledRequest(UUID requestId);
	boolean requestergolems$cancelRequest(UUID requestId);
	void requestergolems$deliverToRequest(UUID requestId, int amount);

	/** Internal transport chunks used by requester golems. */
	boolean requestergolems$hasActiveJobs();
	List<RequesterJob> requestergolems$getActiveJobs();
	RequesterJob requestergolems$claimJob();
	void requestergolems$returnJob(RequesterJob job);
	boolean requestergolems$recoverJob(UUID jobId);
	boolean requestergolems$isJobActive(UUID jobId);
	void requestergolems$completeJob(UUID jobId);
	boolean requestergolems$cancelJob(UUID jobId);

	ItemStack requestergolems$getRequest(int slot);
	void requestergolems$setRequest(int slot, ItemStack stack);
}

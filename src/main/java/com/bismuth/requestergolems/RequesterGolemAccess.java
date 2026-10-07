package com.bismuth.requestergolems;

import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Server-side state contract added to vanilla Copper Golems.
 */
public interface RequesterGolemAccess {
	boolean requestergolems$isRequester();
	void requestergolems$setRequester(boolean requester);

	/**
	 * Marks a carried stack for physical rollback to its source chest.
	 * This state lives on the entity rather than only inside a Brain behavior,
	 * so an interrupted behavior cannot strand the item.
	 */
	BlockPos requestergolems$getRollbackSource();
	void requestergolems$setRollbackSource(BlockPos source);
	void requestergolems$clearRollbackSource();
	UUID requestergolems$getRecoveryJobId();
	BlockPos requestergolems$getRecoveryRequesterPos();
	void requestergolems$setRecoveryState(UUID jobId, BlockPos requesterPos);
	void requestergolems$clearRecoveryState();
}

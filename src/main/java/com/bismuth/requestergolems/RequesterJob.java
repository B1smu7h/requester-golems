package com.bismuth.requestergolems;

import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * A single transactional requester job.
 *
 * <p>The job keeps its identity separate from the item being transported so
 * UI actions can refer to a specific transaction even while a golem is
 * carrying part of it.</p>
 */
public final class RequesterJob {
	public enum State {
		WAITING,
		IN_PROGRESS
	}

	private final UUID id;
	private final UUID requestId;
	private final ItemStack stack;
	private final int originalCount;
	private int consecutiveFailures;
	private State state;

	public RequesterJob(UUID id, UUID requestId, ItemStack stack, State state) {
		this(id, requestId, stack, stack.getCount(), state);
	}

	public RequesterJob(UUID id, UUID requestId, ItemStack stack, int originalCount, State state) {
		this(id, requestId, stack, originalCount, 0, state);
	}

	public RequesterJob(
			UUID id,
			UUID requestId,
			ItemStack stack,
			int originalCount,
			int consecutiveFailures,
			State state
	) {
		this.id = id;
		this.requestId = requestId;
		this.stack = stack.copy();
		this.originalCount = Math.max(this.stack.getCount(), originalCount);
		this.consecutiveFailures = Math.max(0, consecutiveFailures);
		this.state = state;
	}

	public static RequesterJob create(UUID requestId, ItemStack stack) {
		return new RequesterJob(UUID.randomUUID(), requestId, stack, State.WAITING);
	}

	public UUID id() {
		return this.id;
	}

	public UUID requestId() {
		return this.requestId;
	}

	public ItemStack stack() {
		return this.stack;
	}

	public int originalCount() {
		return this.originalCount;
	}

	public int deliveredCount() {
		return this.originalCount - this.stack.getCount();
	}

	public State state() {
		return this.state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public boolean isComplete() {
		return this.stack.isEmpty();
	}

	public int consecutiveFailures() {
		return this.consecutiveFailures;
	}

	public int recordConsecutiveFailure() {
		this.consecutiveFailures++;
		return this.consecutiveFailures;
	}

	public void resetConsecutiveFailures() {
		this.consecutiveFailures = 0;
	}

	public RequesterJob copy() {
		return new RequesterJob(
				this.id,
				this.requestId,
				this.stack,
				this.originalCount,
				this.consecutiveFailures,
				this.state
		);
	}
}

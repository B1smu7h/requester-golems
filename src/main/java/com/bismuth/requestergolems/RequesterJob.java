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
	private final ItemStack stack;
	private final int originalCount;
	private State state;

	public RequesterJob(UUID id, ItemStack stack, State state) {
		this(id, stack, stack.getCount(), state);
	}

	public RequesterJob(UUID id, ItemStack stack, int originalCount, State state) {
		this.id = id;
		this.stack = stack.copy();
		this.originalCount = Math.max(this.stack.getCount(), originalCount);
		this.state = state;
	}

	public static RequesterJob create(ItemStack stack) {
		return new RequesterJob(UUID.randomUUID(), stack, State.WAITING);
	}

	public UUID id() {
		return this.id;
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

	public RequesterJob copy() {
		return new RequesterJob(this.id, this.stack, this.originalCount, this.state);
	}
}

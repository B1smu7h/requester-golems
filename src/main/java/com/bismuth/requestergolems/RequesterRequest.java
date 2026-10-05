package com.bismuth.requestergolems;

import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * A player-facing requester transaction.
 *
 * <p>Transport jobs are internal chunks belonging to this request. The request
 * owns the requested quantity and its creation time, so the UI can represent
 * one logical request even when several golems are transporting it.</p>
 */
public final class RequesterRequest {
	private final UUID id;
	private final ItemStack requestedItem;
	private final int originalCount;
	private int remainingCount;
	private final long createdAt;

	public RequesterRequest(
			UUID id,
			ItemStack requestedItem,
			int originalCount,
			int remainingCount,
			long createdAt
	) {
		this.id = id;
		this.requestedItem = requestedItem.copyWithCount(1);
		this.originalCount = Math.max(0, originalCount);
		this.remainingCount = Math.max(0, Math.min(this.originalCount, remainingCount));
		this.createdAt = createdAt;
	}

	public static RequesterRequest create(ItemStack request, long createdAt) {
		return new RequesterRequest(
				UUID.randomUUID(),
				request,
				request.getCount(),
				request.getCount(),
				createdAt
		);
	}

	public UUID id() {
		return this.id;
	}

	public ItemStack requestedItem() {
		return this.requestedItem;
	}

	public int originalCount() {
		return this.originalCount;
	}

	public int remainingCount() {
		return this.remainingCount;
	}

	public int deliveredCount() {
		return this.originalCount - this.remainingCount;
	}

	public long createdAt() {
		return this.createdAt;
	}

	public boolean isComplete() {
		return this.remainingCount <= 0;
	}

	public void deliver(int amount) {
		this.remainingCount = Math.max(0, this.remainingCount - Math.max(0, amount));
	}

	public RequesterRequest copy() {
		return new RequesterRequest(
				this.id,
				this.requestedItem,
				this.originalCount,
				this.remainingCount,
				this.createdAt
		);
	}
}

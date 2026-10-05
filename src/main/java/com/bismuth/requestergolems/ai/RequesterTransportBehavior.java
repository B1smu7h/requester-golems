package com.bismuth.requestergolems.ai;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import com.bismuth.requestergolems.RequesterJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RequesterTransportBehavior extends Behavior<CopperGolem> {
	// Requester logistics are scoped to a 32x8 search volume around the requester chest.
	// This keeps separate bases from accidentally sharing workers across the server.
	private static final int REQUEST_RANGE_HORIZONTAL = 32;
	private static final int REQUEST_RANGE_VERTICAL = 8;
	private static final int TARGET_INTERACTION_TICKS = 20;
	private static final int RETRY_COOLDOWN_TICKS = 40;

	private BlockPos requesterChestPos;
	private BlockPos sourceChestPos;
	private RequesterJob job;
	private boolean carrying;
	private int interactionTicks;
	private InteractionPhase interactionPhase = InteractionPhase.NONE;
	private int retryCooldownTicks;

	private enum InteractionPhase {
		NONE,
		PICKING_UP,
		DROPPING_OFF
	}

	public RequesterTransportBehavior() {
		super(java.util.Map.of(), 1200);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem body) {
		if (this.retryCooldownTicks > 0) {
			this.retryCooldownTicks--;
			return false;
		}
		if (!(body instanceof RequesterGolemAccess access) || !access.requestergolems$isRequester()) {
			return false;
		}
		return body.getMainHandItem().isEmpty() && this.findRequesterChest(level, body) != null;
	}

	@Override
	protected void start(ServerLevel level, CopperGolem body, long timestamp) {
		this.requesterChestPos = this.findRequesterChest(level, body);
		if (this.requesterChestPos == null) return;

		BlockEntity chestEntity = level.getBlockEntity(this.requesterChestPos);
		if (!(chestEntity instanceof RequesterChestAccess requesterChest)) {
			this.reset();
			return;
		}

		this.job = requesterChest.requestergolems$claimJob();
		if (this.job == null) {
			this.reset();
			return;
		}

		this.sourceChestPos = this.findSourceChest(level, body, this.job.stack());
		if (this.sourceChestPos == null) {
			requesterChest.requestergolems$returnJob(this.job);
			this.reset();
			this.retryCooldownTicks = RETRY_COOLDOWN_TICKS;
			return;
		}

		body.setState(CopperGolemState.IDLE);
		this.moveToContainer(level, body, this.sourceChestPos);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem body, long timestamp) {
		return this.job != null || this.carrying;
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem body, long timestamp) {
		if (this.requesterChestPos == null || (this.job == null && !this.carrying)) return;

		if (this.job != null) {
			BlockEntity jobChestEntity = level.getBlockEntity(this.requesterChestPos);
			if (!(jobChestEntity instanceof RequesterChestAccess jobChest)
					|| !jobChest.requestergolems$isJobActive(this.job.id())
					|| !jobChest.requestergolems$isRequestActive(this.job.requestId())) {
				this.returnCarriedToSource(level, body);
				this.job = null;
				this.carrying = !body.getMainHandItem().isEmpty();
				if (!this.carrying) this.reset();
				return;
			}
		}

		// A cancellation can remove the job while the golem is still carrying
		// items. Finish the rollback path without ever dereferencing a null job.
		if (this.job == null) {
			if (this.carrying) {
				this.returnCarriedToSource(level, body);
				this.carrying = !body.getMainHandItem().isEmpty();
			}
			if (!this.carrying) {
				this.reset();
			}
			return;
		}

		if (!this.carrying) {
			if (this.sourceChestPos == null) {
				this.sourceChestPos = this.findSourceChest(level, body, this.job.stack());
				if (this.sourceChestPos == null) {
					this.failAndRetry(level, body);
					return;
				}
			}

			Vec3 target = Vec3.atCenterOf(this.sourceChestPos);
			if (!this.isInContainerInteractionRange(level, body, this.sourceChestPos)) {
				this.moveToContainer(level, body, this.sourceChestPos);
				return;
			}

			Container source = getContainer(level, this.sourceChestPos, false);
			if (source == null) {
				this.failAndRetry(level, body);
				return;
			}

			if (this.interactionPhase == InteractionPhase.NONE) {
				this.interactionPhase = InteractionPhase.PICKING_UP;
				body.getNavigation().stop();
				this.interactionTicks = TARGET_INTERACTION_TICKS;
				body.setState(CopperGolemState.GETTING_ITEM);
				body.setOpenedChestPos(this.sourceChestPos);
				source.startOpen(body);
				return;
			}

			if (this.interactionPhase == InteractionPhase.PICKING_UP) {
				if (--this.interactionTicks > 0) return;

				int available = Math.max(0, countMatching(source, this.job.stack()) - 1);
				if (available <= 0) {
					source.stopOpen(body);
					body.clearOpenedChestPos();
					body.setState(CopperGolemState.GETTING_NO_ITEM);
					this.interactionPhase = InteractionPhase.NONE;
					this.failAndRetry(level, body);
					return;
				}

				int amount = Math.min(this.job.stack().getCount(), Math.min(16, available));
				ItemStack picked = removeMatching(source, this.job.stack(), amount);
				source.stopOpen(body);
				body.clearOpenedChestPos();
				this.interactionPhase = InteractionPhase.NONE;
				if (picked.isEmpty()) {
					body.setState(CopperGolemState.GETTING_NO_ITEM);
					this.failAndRetry(level, body);
					return;
				}

				body.setItemSlot(EquipmentSlot.MAINHAND, picked);
				body.setState(CopperGolemState.IDLE);
				this.carrying = true;
				body.getNavigation().moveTo(
						this.requesterChestPos.getX() + 0.5,
						this.requesterChestPos.getY(),
						this.requesterChestPos.getZ() + 0.5,
						1.0
				);
				return;
			}
		}

		Vec3 destination = Vec3.atCenterOf(this.requesterChestPos);
		if (!this.isInContainerInteractionRange(level, body, this.requesterChestPos)) {
			this.moveToContainer(level, body, this.requesterChestPos);
			return;
		}

		Container requester = getContainer(level, this.requesterChestPos, true);
		ItemStack carried = body.getMainHandItem();
		if (requester == null || carried.isEmpty()) {
			this.returnCarriedToSource(level, body);
			this.returnJob(level);
			return;
		}

		if (this.interactionPhase == InteractionPhase.NONE) {
			this.interactionPhase = InteractionPhase.DROPPING_OFF;
			body.getNavigation().stop();
			this.interactionTicks = TARGET_INTERACTION_TICKS;
			body.setState(CopperGolemState.DROPPING_ITEM);
			body.setOpenedChestPos(this.requesterChestPos);
			requester.startOpen(body);
			return;
		}

		if (this.interactionPhase != InteractionPhase.DROPPING_OFF || --this.interactionTicks > 0) return;

		ItemStack remainder = insertIntoContainer(requester, carried.copy());
		requester.stopOpen(body);
		body.clearOpenedChestPos();
		this.interactionPhase = InteractionPhase.NONE;
		int delivered = carried.getCount() - remainder.getCount();

		if (delivered <= 0) {
			// The destination accepted nothing. Roll the carried stack back to
			// normal storage instead of hammering a full/blocked requester chest.
			body.setItemSlot(EquipmentSlot.MAINHAND, carried);
			this.carrying = true;
			this.failAndRetry(level, body);
			return;
		}

		BlockEntity destinationEntity = level.getBlockEntity(this.requesterChestPos);
		if (!(destinationEntity instanceof RequesterChestAccess requesterAccess)
				|| !requesterAccess.requestergolems$isRequestActive(this.job.requestId())) {
			// The parent request was cancelled between pickup and delivery. The
			// destination must not receive items for a dead transaction.
			body.setItemSlot(EquipmentSlot.MAINHAND, carried);
			this.carrying = true;
			this.failAndRetry(level, body);
			return;
		}

		// Only successful insertion counts toward the high-level request.
		// Transport chunks remain an implementation detail of the parent request.
		requesterAccess.requestergolems$deliverToRequest(this.job.requestId(), delivered);
		this.job.stack().shrink(delivered);
		if (this.job.isComplete()) {
			requesterAccess.requestergolems$completeJob(this.job.id());
		}
		body.setItemSlot(EquipmentSlot.MAINHAND, remainder);
		this.carrying = !remainder.isEmpty();

		if (this.job.isComplete()) {
			if (!this.carrying) {
				this.reset();
			}
		} else if (!this.carrying) {
			// Partial source fulfillment: put the remaining quantity back
			// into the requester chest job queue.
			this.failAndRetry(level, body);
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem body, long timestamp) {
		if (this.interactionPhase != InteractionPhase.NONE) {
			BlockPos openPos = this.interactionPhase == InteractionPhase.PICKING_UP ? this.sourceChestPos : this.requesterChestPos;
			if (openPos != null) {
				Container openContainer = getContainer(level, openPos, false);
				if (openContainer != null) openContainer.stopOpen(body);
			}
			body.clearOpenedChestPos();
		}
		if (this.carrying) {
			this.returnCarriedToSource(level, body);
		}
		if (this.job != null) {
			this.returnJob(level);
		}
		body.setState(CopperGolemState.IDLE);
		this.reset();
	}

	private boolean isInContainerInteractionRange(ServerLevel level, CopperGolem body, BlockPos containerPos) {
		var state = level.getBlockState(containerPos);
		double range = this.getInteractionRange(body);
		AABB targetBounds = state.getCollisionShape(level, containerPos)
				.bounds()
				.inflate(range, 0.5, range)
				.move(containerPos);
		AABB bodyBounds = AABB.ofSize(
				this.getMiddleYPosition(body),
				body.getBoundingBox().getXsize(),
				body.getBoundingBox().getYsize(),
				body.getBoundingBox().getZsize()
		);
		return targetBounds.intersects(bodyBounds);
	}

	private double getInteractionRange(CopperGolem body) {
		return body.getNavigation().isDone() ? 1.0 : 0.5;
	}

	private Vec3 getMiddleYPosition(CopperGolem body) {
		return body.position().add(0.0, body.getBoundingBox().getYsize() / 2.0, 0.0);
	}

	private void moveToContainer(ServerLevel level, CopperGolem body, BlockPos containerPos) {
		// Match vanilla Copper Golem transport: target the container block itself
		// through the walk/look target memory. The pathfinder chooses the valid
		// adjacent endpoint instead of us inventing a point on/inside the chest.
		BehaviorUtils.setWalkAndLookTargetMemories(body, containerPos, 1.0F, 0);
	}

	private BlockPos findRequesterChest(ServerLevel level, CopperGolem body) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		BlockPos center = body.blockPosition();

		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-REQUEST_RANGE_HORIZONTAL, -REQUEST_RANGE_VERTICAL, -REQUEST_RANGE_HORIZONTAL),
				center.offset(REQUEST_RANGE_HORIZONTAL, REQUEST_RANGE_VERTICAL, REQUEST_RANGE_HORIZONTAL))) {
			BlockEntity entity = level.getBlockEntity(pos);
			if (!(entity instanceof RequesterChestAccess requester)
					|| !requester.requestergolems$isRequester()
					|| !requester.requestergolems$hasActiveRequests()) {
				continue;
			}

			double distance = body.position().distanceToSqr(Vec3.atCenterOf(pos));
			if (distance < bestDistance) {
				bestDistance = distance;
				best = pos.immutable();
			}
		}

		return best;
	}

	private BlockPos findSourceChest(ServerLevel level, CopperGolem body, ItemStack requested) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		BlockPos center = this.requesterChestPos != null ? this.requesterChestPos : body.blockPosition();

		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-REQUEST_RANGE_HORIZONTAL, -REQUEST_RANGE_VERTICAL, -REQUEST_RANGE_HORIZONTAL),
				center.offset(REQUEST_RANGE_HORIZONTAL, REQUEST_RANGE_VERTICAL, REQUEST_RANGE_HORIZONTAL))) {
			BlockEntity entity = level.getBlockEntity(pos);
			if (!(entity instanceof ChestBlockEntity)) continue;

			var state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof ChestBlock) || state.getBlock() instanceof CopperChestBlock) continue;
			if (entity instanceof RequesterChestAccess requester && requester.requestergolems$isRequester()) continue;

			Container container = getContainer(level, pos, false);
			if (container == null || countMatching(container, requested) <= 1) continue;

			double distance = body.position().distanceToSqr(Vec3.atCenterOf(pos));
			if (distance < bestDistance) {
				bestDistance = distance;
				best = pos.immutable();
			}
		}

		return best;
	}

	private static Container getContainer(ServerLevel level, BlockPos pos, boolean ignoreBlocked) {
		var state = level.getBlockState(pos);
		BlockEntity entity = level.getBlockEntity(pos);

		// Requester chests use their existing ChestBlockEntity inventory directly.
		// ChestBlock.getContainer() is for normal chest-block interaction and does
		// not provide the requester copper chest destination used by this behavior.
		if (entity instanceof RequesterChestAccess && entity instanceof Container requesterContainer) {
			return requesterContainer;
		}

		if (!(state.getBlock() instanceof ChestBlock chestBlock)) return null;
		return ChestBlock.getContainer(chestBlock, state, level, pos, ignoreBlocked);
	}

	@FunctionalInterface
	private interface ContainerAction<T> {
		T run();
	}

	private <T> T interactWithContainer(
		CopperGolem body,
		ServerLevel level,
		BlockPos pos,
		Container container,
		ContainerAction<T> action
	) {
		body.setOpenedChestPos(pos);
		container.startOpen(body);
		try {
			return action.run();
		} finally {
			container.stopOpen(body);
			body.clearOpenedChestPos();
		}
	}

	private static int countMatching(Container container, ItemStack requested) {
		int total = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty() && stack.is(requested.getItem())) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static ItemStack removeMatching(Container container, ItemStack requested, int amount) {
		ItemStack result = ItemStack.EMPTY;
		int remaining = amount;

		for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty() || !stack.is(requested.getItem())) continue;

			int taken = Math.min(remaining, stack.getCount());
			ItemStack removed = container.removeItem(slot, taken);
			if (result.isEmpty()) {
				result = removed.copy();
			} else {
				result.grow(removed.getCount());
			}
			remaining -= removed.getCount();
		}

		container.setChanged();
		return result;
	}

	private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			ItemStack existing = container.getItem(slot);
			if (existing.isEmpty() || !existing.is(stack.getItem()) || !container.canPlaceItem(slot, stack)) continue;

			int max = Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize());
			int space = max - existing.getCount();
			if (space <= 0) continue;

			int moved = Math.min(space, stack.getCount());
			existing.grow(moved);
			stack.shrink(moved);
			container.setChanged();
		}

		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack)) continue;

			int moved = Math.min(container.getMaxStackSize(stack), stack.getCount());
			ItemStack placed = stack.split(moved);
			container.setItem(slot, placed);
		}

		return stack;
	}

	private void returnCarriedToSource(ServerLevel level, CopperGolem body) {
		ItemStack carried = body.getMainHandItem();
		if (carried.isEmpty() || this.sourceChestPos == null) return;

		Container source = getContainer(level, this.sourceChestPos, false);
		if (source != null) {
			ItemStack remainder = insertIntoContainer(source, carried.copy());
			body.setItemSlot(EquipmentSlot.MAINHAND, remainder);
		}
	}

	private void failAndRetry(ServerLevel level, CopperGolem body) {
		// Roll back carried items before clearing the active job state.
		if (this.carrying) {
			this.returnCarriedToSource(level, body);
			this.carrying = !body.getMainHandItem().isEmpty();
		}
		this.returnJob(level);
		this.reset();
		this.retryCooldownTicks = RETRY_COOLDOWN_TICKS;
	}

	private void returnJob(ServerLevel level) {
		if (this.job == null || this.requesterChestPos == null) return;

		BlockEntity entity = level.getBlockEntity(this.requesterChestPos);
		if (entity instanceof RequesterChestAccess requester) {
			requester.requestergolems$returnJob(this.job);
		}
		this.job = null;
	}

	private void reset() {
		this.requesterChestPos = null;
		this.sourceChestPos = null;
		this.job = null;
		this.carrying = false;
		this.interactionTicks = 0;
		this.interactionPhase = InteractionPhase.NONE;
	}
}

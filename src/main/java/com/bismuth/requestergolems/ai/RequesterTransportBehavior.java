package com.bismuth.requestergolems.ai;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterGolemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

public class RequesterTransportBehavior extends Behavior<CopperGolem> {
	private static final int SEARCH_HORIZONTAL = 32;
	private static final int SEARCH_VERTICAL = 8;
	private static final double INTERACTION_DISTANCE_SQR = 3.0;

	private BlockPos requesterChestPos;
	private BlockPos sourceChestPos;
	private ItemStack job = ItemStack.EMPTY;
	private boolean carrying;

	public RequesterTransportBehavior() {
		super(java.util.Map.of(), 1200);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, CopperGolem body) {
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
		if (this.job.isEmpty()) {
			this.reset();
			return;
		}

		this.sourceChestPos = this.findSourceChest(level, body, this.job);
		if (this.sourceChestPos == null) {
			requesterChest.requestergolems$returnJob(this.job);
			this.reset();
			return;
		}

		body.getNavigation().moveTo(
				this.sourceChestPos.getX() + 0.5,
				this.sourceChestPos.getY(),
				this.sourceChestPos.getZ() + 0.5,
				1.0
		);
	}

	@Override
	protected boolean canStillUse(ServerLevel level, CopperGolem body, long timestamp) {
		return !this.job.isEmpty() || this.carrying;
	}

	@Override
	protected void tick(ServerLevel level, CopperGolem body, long timestamp) {
		if (this.requesterChestPos == null || this.job.isEmpty()) return;

		if (!this.carrying) {
			if (this.sourceChestPos == null) {
				this.sourceChestPos = this.findSourceChest(level, body, this.job);
				if (this.sourceChestPos == null) {
					this.returnJob(level);
					return;
				}
			}

			Vec3 target = Vec3.atCenterOf(this.sourceChestPos);
			if (body.position().distanceToSqr(target) > INTERACTION_DISTANCE_SQR) {
				body.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
				return;
			}

			Container source = getContainer(level, this.sourceChestPos, false);
			if (source == null) {
				this.returnJob(level);
				return;
			}

			int available = Math.max(0, countMatching(source, this.job) - 1);
			if (available <= 0) {
				this.returnJob(level);
				return;
			}

			int amount = Math.min(this.job.getCount(), Math.min(16, available));
			ItemStack picked = removeMatching(source, this.job, amount);
			if (picked.isEmpty()) {
				this.returnJob(level);
				return;
			}

			body.setItemSlot(EquipmentSlot.MAINHAND, picked);
			this.job.shrink(picked.getCount());
			this.carrying = true;
			body.getNavigation().moveTo(
					this.requesterChestPos.getX() + 0.5,
					this.requesterChestPos.getY(),
					this.requesterChestPos.getZ() + 0.5,
					1.0
			);
			return;
		}

		Vec3 destination = Vec3.atCenterOf(this.requesterChestPos);
		if (body.position().distanceToSqr(destination) > INTERACTION_DISTANCE_SQR) {
			body.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0);
			return;
		}

		Container requester = getContainer(level, this.requesterChestPos, true);
		ItemStack carried = body.getMainHandItem();
		if (requester == null || carried.isEmpty()) {
			this.returnCarriedToSource(level, body);
			this.returnJob(level);
			return;
		}

		ItemStack remainder = insertIntoContainer(requester, carried.copy());
		int delivered = carried.getCount() - remainder.getCount();

		if (delivered > 0) {
			body.setItemSlot(EquipmentSlot.MAINHAND, remainder);
			this.carrying = !remainder.isEmpty();

			if (this.job.isEmpty()) {
				if (!this.carrying) {
					this.reset();
				}
			} else {
				if (this.carrying) {
					// The same physical job is still being delivered; keep walking
					// against the destination until it accepts the stack.
				} else {
					// Partial source fulfillment: put the remaining quantity back
					// into the requester chest job queue.
					this.returnJob(level);
				}
			}
		}
	}

	@Override
	protected void stop(ServerLevel level, CopperGolem body, long timestamp) {
		if (this.carrying) {
			this.returnCarriedToSource(level, body);
		}
		if (!this.job.isEmpty()) {
			this.returnJob(level);
		}
		this.reset();
	}

	private BlockPos findRequesterChest(ServerLevel level, CopperGolem body) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		BlockPos center = body.blockPosition();

		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-SEARCH_HORIZONTAL, -SEARCH_VERTICAL, -SEARCH_HORIZONTAL),
				center.offset(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL))) {
			BlockEntity entity = level.getBlockEntity(pos);
			if (!(entity instanceof RequesterChestAccess requester)
					|| !requester.requestergolems$isRequester()
					|| !requester.requestergolems$hasActiveJobs()) {
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
		BlockPos center = body.blockPosition();

		for (BlockPos pos : BlockPos.betweenClosed(
				center.offset(-SEARCH_HORIZONTAL, -SEARCH_VERTICAL, -SEARCH_HORIZONTAL),
				center.offset(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL))) {
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

	private void returnJob(ServerLevel level) {
		if (this.job.isEmpty() || this.requesterChestPos == null) return;

		BlockEntity entity = level.getBlockEntity(this.requesterChestPos);
		if (entity instanceof RequesterChestAccess requester) {
			requester.requestergolems$returnJob(this.job);
		}
		this.job = ItemStack.EMPTY;
	}

	private void reset() {
		this.requesterChestPos = null;
		this.sourceChestPos = null;
		this.job = ItemStack.EMPTY;
		this.carrying = false;
	}
}

package com.bismuth.requestergolems.menu;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterJob;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class RequesterChestMenu extends ChestMenu {
	public static final int REQUEST_SLOT_COUNT = RequesterChestAccess.REQUEST_SLOT_COUNT;
	private static final int CHEST_SLOT_COUNT = 27;
	private static final int PLAYER_SLOT_COUNT = Inventory.INVENTORY_SIZE;
	private static final int REQUEST_SLOT_START = CHEST_SLOT_COUNT + PLAYER_SLOT_COUNT;
	private static final int ACTIVE_JOB_SLOT_START = REQUEST_SLOT_START + REQUEST_SLOT_COUNT;
	private static final int ACTIVE_JOB_SLOT_COUNT = 10;

	private static final int REQUEST_X = 43;
	private static final int REQUEST_Y = 22;
	private static final int ACTIVE_JOB_LEFT_X = 8;
	private static final int ACTIVE_JOB_RIGHT_X = 96;
	private static final int ACTIVE_JOB_Y = 82;

	private final Container requestContainer;
	private final SimpleContainer activeJobContainer;
	private final ChestBlockEntity requesterChest;
	private final int[] activeJobOriginalCounts = new int[ACTIVE_JOB_SLOT_COUNT];

	public RequesterChestMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(CHEST_SLOT_COUNT), new SimpleContainer(REQUEST_SLOT_COUNT));
	}

	public RequesterChestMenu(int containerId, Inventory inventory, ChestBlockEntity chest) {
		this(containerId, inventory, chest, chest, new RequestContainer(chest));
	}

	private RequesterChestMenu(
			int containerId,
			Inventory inventory,
			ChestBlockEntity chest,
			Container chestContainer,
			Container requestContainer
	) {
		super(ModMenuTypes.REQUESTER_CHEST, containerId, inventory, chestContainer, 3);
		this.requestContainer = requestContainer;
		this.requesterChest = chest;
		this.activeJobContainer = new SimpleContainer(ACTIVE_JOB_SLOT_COUNT);
		for (int slot = 0; slot < ACTIVE_JOB_SLOT_COUNT; slot++) {
			this.addDataSlot(DataSlot.shared(this.activeJobOriginalCounts, slot));
		}

		// ChestMenu gives us the vanilla chest lifecycle and slot semantics.
		// Reposition its existing slots for the taller requester layout without
		// changing their slot indices or containers.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = column + row * 9;
				Slot replacement = new Slot(
						this.getContainer(),
					slot,
					8 + column * 18,
					184 + row * 18
				);
				replacement.index = slot;
				this.slots.set(slot, replacement);
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = 27 + column + row * 9;
				int inventorySlot = 9 + column + row * 9;
				Slot replacement = new Slot(
						inventory,
					inventorySlot,
					8 + column * 18,
					257 + row * 18
				);
				replacement.index = slot;
				this.slots.set(slot, replacement);
			}
		}

		for (int column = 0; column < 9; column++) {
			int slot = 54 + column;
			Slot replacement = new Slot(
					inventory,
					column,
					8 + column * 18,
					311
			);
			replacement.index = slot;
			this.slots.set(slot, replacement);
		}

		for (int column = 0; column < REQUEST_SLOT_COUNT; column++) {
			addSlot(new Slot(requestContainer, column, REQUEST_X + column * 18, REQUEST_Y));
		}

		for (int row = 0; row < 5; row++) {
			for (int column = 0; column < 2; column++) {
				int slot = column + row * 2;
				int x = column == 0 ? ACTIVE_JOB_LEFT_X : ACTIVE_JOB_RIGHT_X;
				addSlot(new Slot(activeJobContainer, slot, x, ACTIVE_JOB_Y + row * 18));
			}
		}
	}

	@Override
	public void broadcastChanges() {
		this.requestergolems$syncActiveJobs();
		super.broadcastChanges();
	}

	public ItemStack requestergolems$getActiveJobStack(int slot) {
		if (slot < 0 || slot >= ACTIVE_JOB_SLOT_COUNT) return ItemStack.EMPTY;
		return this.activeJobContainer.getItem(slot);
	}

	public int requestergolems$getActiveJobOriginalCount(int slot) {
		if (slot < 0 || slot >= ACTIVE_JOB_SLOT_COUNT) return 0;
		return this.activeJobOriginalCounts[slot];
	}

	private void requestergolems$syncActiveJobs() {
		List<RequesterJob> jobs = this.requesterChest instanceof RequesterChestAccess access
				? access.requestergolems$getActiveJobs()
				: List.of();

		for (int slot = 0; slot < ACTIVE_JOB_SLOT_COUNT; slot++) {
			this.activeJobOriginalCounts[slot] = slot < jobs.size() ? jobs.get(slot).originalCount() : 0;
			ItemStack desired = slot < jobs.size() ? jobs.get(slot).stack().copy() : ItemStack.EMPTY;
			ItemStack current = this.activeJobContainer.getItem(slot);
			if (!current.equals(desired)) {
				this.activeJobContainer.setItem(slot, desired);
			}
		}
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex >= ACTIVE_JOB_SLOT_START
				&& slotIndex < ACTIVE_JOB_SLOT_START + ACTIVE_JOB_SLOT_COUNT) {
			if (input == ContainerInput.PICKUP && buttonNum == 0 && getCarried().isEmpty()) {
				this.requestergolems$cancelActiveJob(slotIndex - ACTIVE_JOB_SLOT_START);
			}
			return;
		}

		if (slotIndex >= REQUEST_SLOT_START
				&& slotIndex < REQUEST_SLOT_START + REQUEST_SLOT_COUNT
				&& input == ContainerInput.PICKUP) {
			Slot slot = getSlot(slotIndex);
			ItemStack carried = getCarried();

			if (!carried.isEmpty()) {
				slot.setByPlayer(carried.copy());
			} else {
				slot.setByPlayer(ItemStack.EMPTY);
			}

			broadcastChanges();
			return;
		}

		super.clicked(slotIndex, buttonNum, input, player);
	}

	private void requestergolems$cancelActiveJob(int jobIndex) {
		if (!(this.requesterChest instanceof RequesterChestAccess access)) return;

		List<RequesterJob> jobs = access.requestergolems$getActiveJobs();
		if (jobIndex >= 0 && jobIndex < jobs.size()) {
			access.requestergolems$cancelJob(jobs.get(jobIndex).id());
			broadcastChanges();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) {
			return ItemStack.EMPTY;
		}

		if (slotIndex >= REQUEST_SLOT_START) {
			return ItemStack.EMPTY;
		}

		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();

		if (slotIndex < CHEST_SLOT_COUNT) {
			if (!moveItemStackTo(source, CHEST_SLOT_COUNT, REQUEST_SLOT_START, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!moveItemStackTo(source, 0, CHEST_SLOT_COUNT, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (source.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return copy;
	}

	private static final class RequestContainer extends SimpleContainer {
		private final ChestBlockEntity chest;

		private RequestContainer(ChestBlockEntity chest) {
			super(REQUEST_SLOT_COUNT);
			this.chest = chest;

			if (chest instanceof RequesterChestAccess access) {
				for (int slot = 0; slot < REQUEST_SLOT_COUNT; slot++) {
					super.setItem(slot, access.requestergolems$getRequest(slot));
				}
			}
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			super.setItem(slot, stack);

			if (chest instanceof RequesterChestAccess access) {
				access.requestergolems$setRequest(slot, stack);
			}
		}
	}
}

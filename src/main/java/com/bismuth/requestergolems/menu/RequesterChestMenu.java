package com.bismuth.requestergolems.menu;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.bismuth.requestergolems.RequesterRequest;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class RequesterChestMenu extends ChestMenu {
	private static final int CHEST_SLOT_COUNT = 27;
	public static final int REQUEST_SLOT_COUNT = RequesterChestAccess.REQUEST_SLOT_COUNT;
	private static final int REQUEST_SLOT_START = CHEST_SLOT_COUNT + Inventory.INVENTORY_SIZE;
	public static final int ACTIVE_REQUEST_SLOT_START = REQUEST_SLOT_START + REQUEST_SLOT_COUNT;
	public static final int ACTIVE_REQUEST_SLOT_COUNT = 64;

	private static final int REQUEST_X = 8;
	private static final int REQUEST_Y = 94;

	private final Container requestContainer;
	private final SimpleContainer activeRequestContainer;
	private final ChestBlockEntity requesterChest;
	private final Inventory playerInventory;
	private final int[] activeRequestOriginalCounts = new int[ACTIVE_REQUEST_SLOT_COUNT];
	private final int[] activeRequestRemainingCounts = new int[ACTIVE_REQUEST_SLOT_COUNT];
	private final int[] activeRequestElapsedTicks = new int[ACTIVE_REQUEST_SLOT_COUNT];

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
		this.playerInventory = inventory;
		this.activeRequestContainer = new SimpleContainer(ACTIVE_REQUEST_SLOT_COUNT);

		for (int slot = 0; slot < ACTIVE_REQUEST_SLOT_COUNT; slot++) {
			this.addDataSlot(DataSlot.shared(this.activeRequestOriginalCounts, slot));
			this.addDataSlot(DataSlot.shared(this.activeRequestRemainingCounts, slot));
			this.addDataSlot(DataSlot.shared(this.activeRequestElapsedTicks, slot));
		}

		// Keep the vanilla chest inventory semantics, but place the slots in the
		// compact requester layout.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = column + row * 9;
				Slot replacement = new Slot(
						this.getContainer(),
						slot,
						8 + column * 18,
						24 + row * 18
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
						126 + row * 18
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
						180
			);
			replacement.index = slot;
			this.slots.set(slot, replacement);
		}

		for (int column = 0; column < REQUEST_SLOT_COUNT; column++) {
			addSlot(new Slot(requestContainer, column, REQUEST_X + column * 18, REQUEST_Y));
		}

		// These slots are transport-only backing slots. The client renders the
		// active-request rows itself, so keep the backing slots off-screen while
		// preserving their server-side slot IDs for cancellation clicks.
		for (int slot = 0; slot < ACTIVE_REQUEST_SLOT_COUNT; slot++) {
			addSlot(new Slot(activeRequestContainer, slot, -100, -100));
		}
	}

	public void requestergolems$setSettingsView(boolean settingsView) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = column + row * 9;
				Slot replacement = new Slot(
						this.getContainer(),
						slot,
						settingsView ? -100 : 8 + column * 18,
						settingsView ? -100 : 24 + row * 18
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
						this.playerInventory,
						inventorySlot,
						settingsView ? -100 : 8 + column * 18,
						settingsView ? -100 : 126 + row * 18
				);
				replacement.index = slot;
				this.slots.set(slot, replacement);
			}
		}

		for (int column = 0; column < 9; column++) {
			int slot = 54 + column;
			Slot replacement = new Slot(
						this.playerInventory,
						column,
						settingsView ? -100 : 8 + column * 18,
						settingsView ? -100 : 180
			);
			replacement.index = slot;
			this.slots.set(slot, replacement);
		}

		for (int column = 0; column < REQUEST_SLOT_COUNT; column++) {
			int slot = REQUEST_SLOT_START + column;
			Slot replacement = new Slot(
						this.requestContainer,
						column,
						settingsView ? -100 : REQUEST_X + column * 18,
						settingsView ? -100 : REQUEST_Y
			);
			replacement.index = slot;
			this.slots.set(slot, replacement);
		}
	}

	@Override
	public void broadcastChanges() {
		this.requestergolems$syncActiveRequests();
		super.broadcastChanges();
	}

	public int requestergolems$getActiveRequestCount() {
		int count = 0;
		for (int slot = 0; slot < ACTIVE_REQUEST_SLOT_COUNT; slot++) {
			if (this.activeRequestOriginalCounts[slot] > 0) count++;
		}
		return count;
	}

	public ItemStack requestergolems$getActiveRequestStack(int slot) {
		if (slot < 0 || slot >= ACTIVE_REQUEST_SLOT_COUNT) return ItemStack.EMPTY;
		return this.activeRequestContainer.getItem(slot);
	}

	public int requestergolems$getActiveRequestOriginalCount(int slot) {
		if (slot < 0 || slot >= ACTIVE_REQUEST_SLOT_COUNT) return 0;
		return this.activeRequestOriginalCounts[slot];
	}

	public int requestergolems$getActiveRequestRemainingCount(int slot) {
		if (slot < 0 || slot >= ACTIVE_REQUEST_SLOT_COUNT) return 0;
		return this.activeRequestRemainingCounts[slot];
	}

	public int requestergolems$getActiveRequestElapsedTicks(int slot) {
		if (slot < 0 || slot >= ACTIVE_REQUEST_SLOT_COUNT) return 0;
		return this.activeRequestElapsedTicks[slot];
	}

	private void requestergolems$syncActiveRequests() {
		List<RequesterRequest> requests = this.requesterChest instanceof RequesterChestAccess access
				? access.requestergolems$getActiveRequests()
				: List.of();

		long gameTime = this.requesterChest != null && this.requesterChest.getLevel() != null
				? this.requesterChest.getLevel().getGameTime()
				: 0L;

		for (int slot = 0; slot < ACTIVE_REQUEST_SLOT_COUNT; slot++) {
			if (slot < requests.size()) {
				RequesterRequest request = requests.get(slot);
				this.activeRequestOriginalCounts[slot] = request.originalCount();
				this.activeRequestRemainingCounts[slot] = request.remainingCount();
				long elapsed = Math.max(0L, gameTime - request.createdAt());
				this.activeRequestElapsedTicks[slot] = (int) Math.min(Integer.MAX_VALUE, elapsed);
				this.activeRequestContainer.setItem(
						slot,
						request.requestedItem().copyWithCount(1)
				);
			} else {
				this.activeRequestOriginalCounts[slot] = 0;
				this.activeRequestRemainingCounts[slot] = 0;
				this.activeRequestElapsedTicks[slot] = 0;
				this.activeRequestContainer.setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex >= ACTIVE_REQUEST_SLOT_START
				&& slotIndex < ACTIVE_REQUEST_SLOT_START + ACTIVE_REQUEST_SLOT_COUNT) {
			if (input == ContainerInput.PICKUP && buttonNum == 0 && getCarried().isEmpty()) {
				this.requestergolems$cancelActiveRequest(slotIndex - ACTIVE_REQUEST_SLOT_START);
			}
			return;
		}

		if (slotIndex >= REQUEST_SLOT_START
				&& slotIndex < REQUEST_SLOT_START + REQUEST_SLOT_COUNT
				&& input == ContainerInput.PICKUP) {
			Slot slot = getSlot(slotIndex);
			ItemStack carried = getCarried();
			slot.setByPlayer(carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
			broadcastChanges();
			return;
		}

		super.clicked(slotIndex, buttonNum, input, player);
	}

	private void requestergolems$cancelActiveRequest(int requestIndex) {
		if (!(this.requesterChest instanceof RequesterChestAccess access)) return;

		List<RequesterRequest> requests = access.requestergolems$getActiveRequests();
		if (requestIndex >= 0 && requestIndex < requests.size()) {
			access.requestergolems$cancelRequest(requests.get(requestIndex).id());
			broadcastChanges();
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
		if (slotIndex >= REQUEST_SLOT_START) return ItemStack.EMPTY;

		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();

		if (slotIndex < CHEST_SLOT_COUNT) {
			if (!moveItemStackTo(source, CHEST_SLOT_COUNT, REQUEST_SLOT_START, true)) return ItemStack.EMPTY;
		} else {
			if (!moveItemStackTo(source, 0, CHEST_SLOT_COUNT, false)) return ItemStack.EMPTY;
		}

		if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();

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

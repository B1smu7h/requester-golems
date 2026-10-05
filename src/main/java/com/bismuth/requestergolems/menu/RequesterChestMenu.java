package com.bismuth.requestergolems.menu;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class RequesterChestMenu extends ChestMenu {
	public static final int REQUEST_SLOT_COUNT = RequesterChestAccess.REQUEST_SLOT_COUNT;
	private static final int CHEST_SLOT_COUNT = 27;
	private static final int PLAYER_SLOT_COUNT = Inventory.INVENTORY_SIZE;
	private static final int REQUEST_SLOT_START = CHEST_SLOT_COUNT + PLAYER_SLOT_COUNT;

	private static final int REQUEST_X = 43;
	private static final int REQUEST_Y = 22;

	private final Container requestContainer;

	public RequesterChestMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(CHEST_SLOT_COUNT), new SimpleContainer(REQUEST_SLOT_COUNT));
	}

	public RequesterChestMenu(int containerId, Inventory inventory, ChestBlockEntity chest) {
		this(containerId, inventory, chest, new RequestContainer(chest));
	}

	private RequesterChestMenu(
			int containerId,
			Inventory inventory,
			Container chestContainer,
			Container requestContainer
	) {
		super(ModMenuTypes.REQUESTER_CHEST, containerId, inventory, chestContainer, 3);
		this.requestContainer = requestContainer;

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
						80 + row * 18
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
						135 + row * 18
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
					193
			);
			replacement.index = slot;
			this.slots.set(slot, replacement);
		}

		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 5; column++) {
				int slot = column + row * 5;
				addSlot(new Slot(requestContainer, slot, REQUEST_X + column * 18, REQUEST_Y + row * 18));
			}
		}
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
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

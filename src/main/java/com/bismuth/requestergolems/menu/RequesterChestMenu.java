package com.bismuth.requestergolems.menu;

import com.bismuth.requestergolems.RequesterChestAccess;
import com.mojang.logging.LogUtils;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.slf4j.Logger;

public class RequesterChestMenu extends AbstractContainerMenu {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int REQUEST_SLOT_COUNT = RequesterChestAccess.REQUEST_SLOT_COUNT;
	public static final int CHEST_SLOT_START = REQUEST_SLOT_COUNT;
	public static final int CHEST_SLOT_END = CHEST_SLOT_START + 27;
	public static final int PLAYER_SLOT_START = CHEST_SLOT_END;
	public static final int PLAYER_SLOT_END = PLAYER_SLOT_START + Inventory.INVENTORY_SIZE;

	private static final int REQUEST_X = 43;
	private static final int REQUEST_Y = 22;
	private static final int CHEST_X = 8;
	private static final int CHEST_Y = 80;
	private static final int INVENTORY_X = 8;
	private static final int INVENTORY_Y = 135;

	private final Container requestContainer;
	private final Container chestContainer;

	public RequesterChestMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(REQUEST_SLOT_COUNT), new SimpleContainer(27), null);
	}

	public RequesterChestMenu(int containerId, Inventory inventory, ChestBlockEntity chest) {
		this(containerId, inventory, new RequestContainer(chest), chest, chest);
	}

	private RequesterChestMenu(
			int containerId,
			Inventory inventory,
			Container requestContainer,
			Container chestContainer,
			ChestBlockEntity chest
	) {
		super(ModMenuTypes.REQUESTER_CHEST, containerId);
		checkContainerSize(requestContainer, REQUEST_SLOT_COUNT);
		checkContainerSize(chestContainer, 27);
		this.requestContainer = requestContainer;
		this.chestContainer = chestContainer;


		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 5; column++) {
				int slot = column + row * 5;
				addSlot(new Slot(requestContainer, slot, REQUEST_X + column * 18, REQUEST_Y + row * 18));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int slot = column + row * 9;
				addSlot(new Slot(chestContainer, slot, CHEST_X + column * 18, CHEST_Y + row * 18));
			}
		}

		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < REQUEST_SLOT_COUNT && input == ContainerInput.PICKUP) {
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

		Slot slot = slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack source = slot.getItem();
		ItemStack copy = source.copy();

		if (slotIndex < REQUEST_SLOT_COUNT) {
			return ItemStack.EMPTY;
		}

		if (slotIndex >= PLAYER_SLOT_START) {
			if (!moveItemStackTo(source, CHEST_SLOT_START, CHEST_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!moveItemStackTo(source, PLAYER_SLOT_START, PLAYER_SLOT_END, false)) {
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

	@Override
	public boolean stillValid(Player player) {
		return chestContainer.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		LOGGER.info("RequesterChestMenu removed for {} (server={})", player.getName().getString(), !player.level().isClientSide());
		super.removed(player);
		if (chestContainer instanceof ChestBlockEntity chest) {
			LOGGER.info("RequesterChestMenu calling stopOpen on {}", chest.getBlockPos());
			chest.stopOpen(player);
		}
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

package com.bismuth.requestergolems;

import net.minecraft.world.item.ItemStack;

/**
 * Persistent role and request-slot state layered onto a vanilla chest.
 */
public interface RequesterChestAccess {
	int REQUEST_SLOT_COUNT = 10;

	boolean requestergolems$isRequester();

	void requestergolems$setRequester(boolean requester);

	ItemStack requestergolems$getRequest(int slot);

	void requestergolems$setRequest(int slot, ItemStack stack);
}

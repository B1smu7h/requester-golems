package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.NonNullList;

/**
 * Adds requester-mode metadata and ten persistent request slots to vanilla
 * chest block entities.
 *
 * <p>The underlying 27-slot chest inventory remains untouched. Request slots
 * are a separate data model and will later be presented by the requester
 * chest menu.</p>
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements RequesterChestAccess {
	private static final String REQUESTER_KEY = "requestergolems:requester";
	private static final String REQUESTS_KEY = "requestergolems:requests";

	private boolean requestergolems$requester;
	private final NonNullList<ItemStack> requestergolems$requests =
			NonNullList.withSize(RequesterChestAccess.REQUEST_SLOT_COUNT, ItemStack.EMPTY);

	@Override
	public boolean requestergolems$isRequester() {
		return this.requestergolems$requester;
	}

	@Override
	public void requestergolems$setRequester(boolean requester) {
		this.requestergolems$requester = requester;
	}

	@Override
	public ItemStack requestergolems$getRequest(int slot) {
		if (slot < 0 || slot >= RequesterChestAccess.REQUEST_SLOT_COUNT) {
			throw new IndexOutOfBoundsException("Invalid requester slot: " + slot);
		}
		return this.requestergolems$requests.get(slot);
	}

	@Override
	public void requestergolems$setRequest(int slot, ItemStack stack) {
		if (slot < 0 || slot >= RequesterChestAccess.REQUEST_SLOT_COUNT) {
			throw new IndexOutOfBoundsException("Invalid requester slot: " + slot);
		}

		this.requestergolems$requests.set(slot, stack.copy());
		((ChestBlockEntity) (Object) this).setChanged();
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);

		ValueOutput.TypedOutputList<ItemStack> requests =
				output.list(REQUESTS_KEY, ItemStack.CODEC);

		for (ItemStack request : this.requestergolems$requests) {
			requests.add(request);
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);

		var requests = input.listOrEmpty(REQUESTS_KEY, ItemStack.CODEC);
		int slot = 0;
		for (ItemStack request : requests) {
			if (slot >= RequesterChestAccess.REQUEST_SLOT_COUNT) {
				break;
			}

			this.requestergolems$requests.set(slot++, request);
		}

		while (slot < RequesterChestAccess.REQUEST_SLOT_COUNT) {
			this.requestergolems$requests.set(slot++, ItemStack.EMPTY);
		}
	}
}

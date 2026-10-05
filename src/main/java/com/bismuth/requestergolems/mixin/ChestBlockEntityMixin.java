package com.bismuth.requestergolems.mixin;

import com.bismuth.requestergolems.RequesterChestAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
	private static final String REQUEST_KEY_PREFIX = "requestergolems:request_";

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

	@Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
	private void requestergolems$createRequesterMenu(
			int containerId,
			Inventory inventory,
			CallbackInfoReturnable<AbstractContainerMenu> cir
	) {
		if (this.requestergolems$requester) {
			cir.setReturnValue(new RequesterChestMenu(containerId, inventory, (ChestBlockEntity) (Object) this));
		}
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void requestergolems$save(ValueOutput output, CallbackInfo ci) {
		output.putBoolean(REQUESTER_KEY, this.requestergolems$requester);

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			ItemStack request = this.requestergolems$requests.get(slot);
			if (!request.isEmpty()) {
				output.store(REQUEST_KEY_PREFIX + slot, ItemStack.CODEC, request);
			}
		}
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void requestergolems$load(ValueInput input, CallbackInfo ci) {
		this.requestergolems$requester = input.getBooleanOr(REQUESTER_KEY, false);

		for (int slot = 0; slot < RequesterChestAccess.REQUEST_SLOT_COUNT; slot++) {
			this.requestergolems$requests.set(
					slot,
					input.read(REQUEST_KEY_PREFIX + slot, ItemStack.CODEC).orElse(ItemStack.EMPTY)
			);
		}
	}
}

package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RequesterChestScreen extends AbstractContainerScreen<RequesterChestMenu> {
	public RequesterChestScreen(RequesterChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, Component.translatable("container.requestergolems.requester_chest"), 176, 340);
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 244;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);

		// Request area.
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 60, 0xFF8B8B8B);
		graphics.text(
				this.font,
				Component.translatable("container.requestergolems.requests"),
				this.leftPos + 104,
				this.topPos + 6,
				0xFF404040,
				false
		);

		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 5; column++) {
				drawSlot(graphics, this.leftPos + 43 + column * 18, this.topPos + 22 + row * 18);
			}
		}

		// Active jobs. Each row is a transaction with its current progress.
		graphics.fill(this.leftPos + 4, this.topPos + 64, this.leftPos + this.imageWidth - 4, this.topPos + 176, 0xFF8B8B8B);
		Component activeRequestsLabel = Component.translatable("container.requestergolems.active_requests");
		graphics.text(
				this.font,
				activeJobsLabel,
				this.leftPos + (this.imageWidth - this.font.width(activeJobsLabel)) / 2,
				this.topPos + 66,
				0xFF404040,
				false
		);

		for (int row = 0; row < 5; row++) {
			drawSlot(graphics, this.leftPos + 8, this.topPos + 82 + row * 18);
			drawSlot(graphics, this.leftPos + 96, this.topPos + 82 + row * 18);
		}

		// Physical chest inventory.
		graphics.fill(this.leftPos + 4, this.topPos + 180, this.leftPos + this.imageWidth - 4, this.topPos + 237, 0xFF8B8B8B);

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 184 + row * 18);
			}
		}

		// Player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 257 + row * 18);
			}
		}

		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 311);
		}
	}

	private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
		graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFFE8E8E8);
		graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFFE8E8E8);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);

		graphics.text(
				this.font,
				this.playerInventoryTitle,
				this.inventoryLabelX,
				this.inventoryLabelY,
				0xFF404040,
				false
		);

		for (int row = 0; row < 5; row++) {
			for (int column = 0; column < 2; column++) {
				int slot = column + row * 2;
				ItemStack requestStack = this.menu.requestergolems$getActiveRequestStack(slot);
				if (requestStack.isEmpty()) continue;

				int x = column == 0 ? 8 : 96;
				int y = 82 + row * 18;
				int original = this.menu.requestergolems$getActiveRequestOriginalCount(slot);
				int remaining = this.menu.requestergolems$getActiveRequestRemainingCount(slot);
				int delivered = original - remaining;
				String progress = delivered + "/" + original;

				graphics.text(
						this.font,
						Component.literal(progress),
						x + 20,
						y + 5,
						0xFF404040,
						false
				);
			}
		}
	}
}

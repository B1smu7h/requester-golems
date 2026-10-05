package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RequesterChestScreen extends AbstractContainerScreen<RequesterChestMenu> {
	public RequesterChestScreen(RequesterChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, Component.translatable("container.requestergolems.requester_chest"), 176, 222);
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 112;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Simple vanilla-style first-pass background. The actual item rendering
		// is handled by AbstractContainerScreen after this method returns.
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);

		// Request area.
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 60, 0xFF8B8B8B);
		graphics.centeredText(
				this.font,
				Component.translatable("container.requestergolems.requests"),
				this.leftPos + this.imageWidth / 2,
				this.topPos + 6,
				0x404040
		);

		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 5; column++) {
				drawSlot(graphics, this.leftPos + 43 + column * 18, this.topPos + 22 + row * 18);
			}
		}

		// Physical chest inventory.
		graphics.fill(this.leftPos + 4, this.topPos + 64, this.leftPos + this.imageWidth - 4, this.topPos + 121, 0xFF8B8B8B);
		graphics.text(
				this.font,
				Component.translatable("container.requestergolems.storage"),
				this.leftPos + 8,
				this.topPos + 65,
				0x404040
		);

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 79 + row * 18);
			}
		}

		// Player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 135 + row * 18);
			}
		}

		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 191);
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
		graphics.text(
				this.font,
				this.playerInventoryTitle,
				this.leftPos + this.inventoryLabelX,
				this.topPos + this.inventoryLabelY,
				0x404040
		);
	}
}

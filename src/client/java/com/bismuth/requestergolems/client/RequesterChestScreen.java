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
		super.extractBackground(graphics, mouseX, mouseY, delta);

		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 58, 0xFF8B8B8B);
		graphics.fill(this.leftPos + 4, this.topPos + 62, this.leftPos + this.imageWidth - 4, this.topPos + 120, 0xFF8B8B8B);
		graphics.fill(this.leftPos + 4, this.topPos + 120, this.leftPos + this.imageWidth - 4, this.topPos + 121, 0xFF555555);

		graphics.centeredText(this.font,
				Component.translatable("container.requestergolems.requests"),
				this.leftPos + this.imageWidth / 2,
				this.topPos + 7,
				0x404040);
		graphics.centeredText(this.font,
				Component.translatable("container.requestergolems.storage"),
				this.leftPos + this.imageWidth / 2,
				this.topPos + 51,
				0x404040);

	}
}

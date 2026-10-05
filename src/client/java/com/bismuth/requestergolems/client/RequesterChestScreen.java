package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.RequesterGolems;
import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class RequesterChestScreen extends AbstractContainerScreen<RequesterChestMenu> {
	private static final Identifier BACKGROUND =
			Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

	public RequesterChestScreen(RequesterChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, Component.translatable("container.requestergolems.requester_chest"), 176, 222);
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 112;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 58, 0xFF8B8B8B);
		graphics.fill(this.leftPos + 4, this.topPos + 62, this.leftPos + this.imageWidth - 4, this.topPos + 120, 0xFF8B8B8B);
		graphics.fill(this.leftPos + 4, this.topPos + 120, this.leftPos + this.imageWidth - 4, this.topPos + 121, 0xFF555555);
		super.extractBackground(graphics, mouseX, mouseY, delta);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, Component.translatable("container.requestergolems.requester_chest"), this.leftPos + this.titleLabelX, this.topPos + this.titleLabelY, 0x404040);
		graphics.text(this.font, Component.translatable("container.requestergolems.storage"), this.leftPos + this.inventoryLabelX, this.topPos + 49, 0x404040);
		graphics.text(this.font, this.playerInventoryTitle, this.leftPos + this.inventoryLabelX, this.topPos + this.inventoryLabelY, 0x404040);
	}
}

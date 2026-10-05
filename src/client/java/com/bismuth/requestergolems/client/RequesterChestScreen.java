package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.RequesterChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class RequesterChestScreen extends AbstractContainerScreen<RequesterChestMenu> {
	private static final int MAIN_HEIGHT = 208;
	private static final int SETTINGS_HEIGHT = 184;
	private static final int SETTINGS_LIST_TOP = 28;
	private static final int SETTINGS_LIST_BOTTOM = 164;
	private static final int SETTINGS_ROW_HEIGHT = 20;
	private static final int SETTINGS_VISIBLE_ROWS = 6;
	private static final int SETTINGS_SCROLLBAR_X = 168;
	private static final int SETTINGS_SCROLLBAR_WIDTH = 5;

	private boolean settingsMode;
	private int scrollOffset;
	private Button modeButton;

	public RequesterChestScreen(RequesterChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, Component.translatable("container.requestergolems.requester_chest"),
				176, MAIN_HEIGHT);
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 116;
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();

		if (this.settingsMode) {
			this.imageHeight = SETTINGS_HEIGHT;
			this.modeButton = this.addRenderableWidget(
					Button.builder(Component.literal("<"), button -> this.setSettingsMode(false))
							.bounds(this.leftPos + 148, this.topPos + 4, 24, 20)
							.build()
			);
		} else {
			this.imageHeight = MAIN_HEIGHT;
			this.modeButton = this.addRenderableWidget(
					Button.builder(Component.literal("⚙"), button -> this.setSettingsMode(true))
							.bounds(this.leftPos + 148, this.topPos + 4, 24, 20)
							.build()
			);
		}
	}

	private void setSettingsMode(boolean settings) {
		this.settingsMode = settings;
		this.scrollOffset = 0;
		this.imageHeight = settings ? SETTINGS_HEIGHT : MAIN_HEIGHT;
		this.rebuildWidgets();
	}

	@Override
	protected void repositionElements() {
		super.repositionElements();
		this.imageHeight = this.settingsMode ? SETTINGS_HEIGHT : MAIN_HEIGHT;
	}

	@Override
	protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!this.settingsMode) {
			super.extractSlots(graphics, mouseX, mouseY);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);

		if (this.settingsMode) {
			this.extractSettingsBackground(graphics, mouseX, mouseY);
			return;
		}

		// Header.
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 22, 0xFF8B8B8B);

		// Physical requester chest inventory.
		graphics.fill(this.leftPos + 4, this.topPos + 22, this.leftPos + this.imageWidth - 4, this.topPos + 80, 0xFF8B8B8B);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 24 + row * 18);
			}
		}

		// Request row.
		graphics.fill(this.leftPos + 4, this.topPos + 82, this.leftPos + this.imageWidth - 4, this.topPos + 114, 0xFF8B8B8B);
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 94);
		}

		// Player inventory.
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 126 + row * 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 180);
		}
	}

	private void extractSettingsBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + 24, 0xFF8B8B8B);
		graphics.fill(this.leftPos + 4, this.topPos + SETTINGS_LIST_TOP,
				this.leftPos + this.imageWidth - 4, this.topPos + SETTINGS_LIST_BOTTOM, 0xFF8B8B8B);

		int count = this.menu.requestergolems$getActiveRequestCount();
		boolean scrollable = count > SETTINGS_VISIBLE_ROWS;

		for (int visible = 0; visible < SETTINGS_VISIBLE_ROWS; visible++) {
			int index = this.scrollOffset + visible;
			if (index >= count) break;

			int y = this.topPos + SETTINGS_LIST_TOP + visible * SETTINGS_ROW_HEIGHT;
			boolean hovered = mouseX >= this.leftPos + 6 && mouseX < this.leftPos + 164
					&& mouseY >= y && mouseY < y + SETTINGS_ROW_HEIGHT;
			graphics.fill(
					this.leftPos + 6,
					y,
					this.leftPos + 164,
					y + SETTINGS_ROW_HEIGHT - 1,
					hovered ? 0xFF707070 : 0xFF777777
			);
		}

		// Reserve scrollbar space even when there is nothing to scroll.
		graphics.fill(
				this.leftPos + SETTINGS_SCROLLBAR_X,
				this.topPos + SETTINGS_LIST_TOP,
				this.leftPos + SETTINGS_SCROLLBAR_X + SETTINGS_SCROLLBAR_WIDTH,
				this.topPos + SETTINGS_LIST_BOTTOM,
				0xFF6B6B6B
		);

		if (scrollable) {
			int trackHeight = SETTINGS_LIST_BOTTOM - SETTINGS_LIST_TOP;
			int thumbHeight = Math.max(18, trackHeight * SETTINGS_VISIBLE_ROWS / count);
			int maxScroll = count - SETTINGS_VISIBLE_ROWS;
			int thumbY = SETTINGS_LIST_TOP
					+ (trackHeight - thumbHeight) * this.scrollOffset / maxScroll;
			graphics.fill(
					this.leftPos + SETTINGS_SCROLLBAR_X,
					this.topPos + thumbY,
					this.leftPos + SETTINGS_SCROLLBAR_X + SETTINGS_SCROLLBAR_WIDTH,
					this.topPos + thumbY + thumbHeight,
					0xFFC0C0C0
			);
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
		if (this.settingsMode) {
			Component title = Component.translatable("container.requestergolems.active_requests");
			graphics.text(this.font, title,
					this.leftPos + 8, this.topPos + 8, 0xFF404040, false);

			int count = this.menu.requestergolems$getActiveRequestCount();
			for (int visible = 0; visible < SETTINGS_VISIBLE_ROWS; visible++) {
				int index = this.scrollOffset + visible;
				if (index >= count) break;

				ItemStack stack = this.menu.requestergolems$getActiveRequestStack(index);
				if (stack.isEmpty()) continue;

				int y = this.topPos + SETTINGS_LIST_TOP + visible * SETTINGS_ROW_HEIGHT + 2;
				graphics.item(stack, this.leftPos + 9, y);
				graphics.itemDecorations(this.font, stack, this.leftPos + 9, y);

				String name = stack.getHoverName().getString();
				if (name.length() > 18) name = name.substring(0, 17) + "…";
				graphics.text(this.font, Component.literal(name),
						this.leftPos + 30, y + 3, 0xFF404040, false);

				graphics.text(this.font, Component.literal(
						formatElapsed(this.menu.requestergolems$getActiveRequestElapsedTicks(index))),
					this.leftPos + 100, y + 3, 0xFF404040, false);

				int delivered = this.menu.requestergolems$getActiveRequestOriginalCount(index)
						- this.menu.requestergolems$getActiveRequestRemainingCount(index);
				graphics.text(this.font, Component.literal(
						delivered + "/" + this.menu.requestergolems$getActiveRequestOriginalCount(index)),
						this.leftPos + 136, y + 3, 0xFF404040, false);
			}

			return;
		}

		graphics.text(this.font, this.title, this.leftPos + this.titleLabelX, this.topPos + this.titleLabelY, 0xFF404040, false);
		graphics.text(this.font, Component.translatable("container.requestergolems.requests"),
				this.leftPos + 104, this.topPos + 86, 0xFF404040, false);
		graphics.text(this.font, this.playerInventoryTitle,
				this.leftPos + this.inventoryLabelX, this.topPos + this.inventoryLabelY, 0xFF404040, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.settingsMode && this.isInsideSettingsList(mouseX, mouseY)) {
			int visible = (int)((mouseY - (this.topPos + SETTINGS_LIST_TOP)) / SETTINGS_ROW_HEIGHT);
			int index = this.scrollOffset + visible;
			if (visible >= 0 && visible < SETTINGS_VISIBLE_ROWS
					&& index < this.menu.requestergolems$getActiveRequestCount()
					&& !this.menu.requestergolems$getActiveRequestStack(index).isEmpty()) {
				graphics.setTooltipForNextFrame(
						Component.translatable("container.requestergolems.cancel_request"),
						mouseX,
						mouseY
				);
				return;
			}
		}
		super.extractTooltip(graphics, mouseX, mouseY);
	}

	private static String formatElapsed(int ticks) {
		int totalSeconds = Math.max(0, ticks) / 20;
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;
		return hours > 0
				? String.format("%02d:%02d:%02d", hours, minutes, seconds)
				: String.format("%02d:%02d", minutes, seconds);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.settingsMode && event.button() == 0
				&& this.isInsideSettingsList(event.x(), event.y())) {
			int visible = (int)((event.y() - (this.topPos + SETTINGS_LIST_TOP)) / SETTINGS_ROW_HEIGHT);
			if (visible >= 0 && visible < SETTINGS_VISIBLE_ROWS) {
				int index = this.scrollOffset + visible;
				if (index < this.menu.requestergolems$getActiveRequestCount()
						&& this.minecraft.gameMode != null && this.minecraft.player != null) {
					this.minecraft.gameMode.handleContainerInput(
							this.menu.containerId,
							RequesterChestMenu.ACTIVE_REQUEST_SLOT_START + index,
							0,
							ContainerInput.PICKUP,
							this.minecraft.player
					);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (this.settingsMode && this.isInsideSettingsList(x, y)) {
			int count = this.menu.requestergolems$getActiveRequestCount();
			int maxScroll = Math.max(0, count - SETTINGS_VISIBLE_ROWS);
			if (maxScroll > 0) {
				this.scrollOffset = Mth.clamp(
						this.scrollOffset - (int)Math.signum(scrollY),
						0,
						maxScroll
				);
				return true;
			}
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private boolean isInsideSettingsList(double mouseX, double mouseY) {
		return mouseX >= this.leftPos + 6 && mouseX < this.leftPos + 164
				&& mouseY >= this.topPos + SETTINGS_LIST_TOP
				&& mouseY < this.topPos + SETTINGS_LIST_BOTTOM;
	}
}

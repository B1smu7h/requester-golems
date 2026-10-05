package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.RequesterChestMenu;
import com.bismuth.requestergolems.network.CancelRequesterRequestPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RequesterChestScreen extends AbstractContainerScreen<RequesterChestMenu> {
	private static final Identifier CONTAINER_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final int MAIN_HEIGHT = 210;
	private static final int SETTINGS_LIST_TOP = 28;
	private static final int SETTINGS_LIST_BOTTOM = 92;
	private static final int SETTINGS_ROW_HEIGHT = 20;
	private static final int SETTINGS_VISIBLE_ROWS = 3;
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
		this.inventoryLabelY = 117;
	}

	@Override
	protected void init() {
		super.init();
		this.clearWidgets();

		if (this.settingsMode) {
			this.modeButton = this.addRenderableWidget(
					Button.builder(Component.literal("<"), button -> this.setSettingsMode(false))
							.bounds(this.leftPos + 154, this.topPos + 2, 18, 16)
							.build()
			);
		} else {
			this.modeButton = this.addRenderableWidget(
					Button.builder(Component.literal("⚙"), button -> this.setSettingsMode(true))
							.bounds(this.leftPos + 154, this.topPos + 2, 18, 16)
							.build()
			);
		}
	}

	private void setSettingsMode(boolean settings) {
		this.settingsMode = settings;
		this.scrollOffset = 0;
		this.menu.requestergolems$setSettingsView(settings);
		this.rebuildWidgets();
	}


	@Override
	protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractSlots(graphics, mouseX, mouseY);
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
		boolean activeRequestSlot = slot.index >= RequesterChestMenu.ACTIVE_REQUEST_SLOT_START
				&& slot.index < RequesterChestMenu.ACTIVE_REQUEST_SLOT_START + RequesterChestMenu.ACTIVE_REQUEST_SLOT_COUNT;

		if (this.settingsMode != activeRequestSlot) {
			return;
		}

		super.extractSlot(graphics, slot, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (this.settingsMode) {
			graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
			this.extractSettingsBackground(graphics, mouseX, mouseY);
			return;
		}

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				CONTAINER_TEXTURE,
				this.leftPos,
				this.topPos,
				0,
				0,
				this.imageWidth,
				71,
				256,
				256
		);

		// The request row is inserted below the vanilla 3-row chest section.
		// Do not sample rows 4-5 of generic_54 for the gap: those pixels contain
		// more slot artwork, which is what caused the fake/void-looking region.
		// Extend the vanilla container's side walls through the requester area.
		// The center stays plain GUI background; only the left/right frame continues.
		graphics.fill(
				this.leftPos,
				this.topPos + 71,
				this.leftPos + this.imageWidth,
				this.topPos + 114,
				0xFFC6C6C6
		);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				CONTAINER_TEXTURE,
				this.leftPos,
				this.topPos + 71,
				0,
				17,
				7,
				43,
				256,
				256
		);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				CONTAINER_TEXTURE,
				this.leftPos + this.imageWidth - 7,
				this.topPos + 71,
				169,
				17,
				7,
				43,
				256,
				256
		);

		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				CONTAINER_TEXTURE,
				this.leftPos,
				this.topPos + 94,
				0,
				17,
				this.imageWidth,
				18,
				256,
				256
		);



		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				CONTAINER_TEXTURE,
				this.leftPos,
				this.topPos + 114,
				0,
				126,
				this.imageWidth,
				96,
				256,
				256
		);

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

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.settingsMode) {
			Component title = Component.translatable("container.requestergolems.active_requests");
			graphics.text(this.font, title,
					8, 8, 0xFF404040, false);

			int count = this.menu.requestergolems$getActiveRequestCount();
			for (int visible = 0; visible < SETTINGS_VISIBLE_ROWS; visible++) {
				int index = this.scrollOffset + visible;
				if (index >= count) break;

				ItemStack stack = this.menu.requestergolems$getActiveRequestStack(index);
				if (stack.isEmpty()) continue;

				int y = SETTINGS_LIST_TOP + visible * SETTINGS_ROW_HEIGHT + 2;
								String name = stack.getHoverName().getString();
				if (name.length() > 18) name = name.substring(0, 17) + "…";
				graphics.text(this.font, Component.literal(name),
						30, y + 3, 0xFF404040, false);

				graphics.text(this.font, Component.literal(
						formatElapsed(this.menu.requestergolems$getActiveRequestElapsedTicks(index))),
					100, y + 3, 0xFF404040, false);

				int delivered = this.menu.requestergolems$getActiveRequestOriginalCount(index)
						- this.menu.requestergolems$getActiveRequestRemainingCount(index);
				graphics.text(this.font, Component.literal(
						delivered + "/" + this.menu.requestergolems$getActiveRequestOriginalCount(index)),
						136, y + 3, 0xFF404040, false);
			}

			return;
		}

		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
		graphics.text(this.font, Component.translatable("container.requestergolems.requests"),
				8, 77, 0xFF404040, false);
		graphics.text(this.font, this.playerInventoryTitle,
				this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.settingsMode) {
			if (this.isInsideSettingsList(mouseX, mouseY)) {
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
				}
			}
			return;
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
		if (this.settingsMode) {
			if (this.isInsideModeButton(event.x(), event.y())) {
				return super.mouseClicked(event, doubleClick);
			}

			if (event.button() == 1 && this.isInsideSettingsList(event.x(), event.y())) {
				int visible = (int)((event.y() - (this.topPos + SETTINGS_LIST_TOP)) / SETTINGS_ROW_HEIGHT);
				if (visible >= 0 && visible < SETTINGS_VISIBLE_ROWS) {
					int index = this.scrollOffset + visible;
					if (index < this.menu.requestergolems$getActiveRequestCount()
							&& this.minecraft.gameMode != null && this.minecraft.player != null) {
						ClientPlayNetworking.send(new CancelRequesterRequestPayload(index));
						return true;
					}
				}
			}
			return true;
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
				this.menu.requestergolems$setActiveRequestView(this.scrollOffset);
				return true;
			}
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private boolean isInsideModeButton(double mouseX, double mouseY) {
		return mouseX >= this.leftPos + 154 && mouseX < this.leftPos + 172
				&& mouseY >= this.topPos + 2 && mouseY < this.topPos + 18;
	}

	private boolean isInsideSettingsList(double mouseX, double mouseY) {
		return mouseX >= this.leftPos + 6 && mouseX < this.leftPos + 164
				&& mouseY >= this.topPos + SETTINGS_LIST_TOP
				&& mouseY < this.topPos + SETTINGS_LIST_BOTTOM;
	}
}

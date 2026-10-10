package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.network.RenameRequesterGolemPayload;
import com.bismuth.requestergolems.network.UnassignAllRequesterGolemPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RequesterGolemScreen extends Screen {
	private final int publicId;
	private final String currentName;
	private final boolean canEdit;
	private final boolean canClaim;
	private final boolean global;
	private EditBox nameField;

	public RequesterGolemScreen(int publicId, String currentName, boolean canEdit, boolean canClaim, boolean global) {
		super(Component.literal("Requester Golem"));
		this.publicId = publicId;
		this.currentName = currentName == null ? "Requester Golem" : currentName;
		this.canEdit = canEdit;
		this.canClaim = canClaim;
		this.global = global;
	}

	@Override
	protected void init() {
		int center = this.width / 2;
		if (this.canEdit) {
			this.nameField = this.addRenderableWidget(new EditBox(
					this.font, center - 100, this.height / 2 - 5, 200, 20,
					Component.literal("Golem name")
			));
			this.nameField.setMaxLength(32);
			this.nameField.setValue(this.currentName);
			this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
				ClientPlayNetworking.send(new RenameRequesterGolemPayload(this.publicId, this.nameField.getValue()));
				this.minecraft.gui.setScreen(null);
			}).bounds(center - 100, this.height / 2 + 22, this.global ? 96 : 62, 20).build());
			if (!this.global) {
				this.addRenderableWidget(Button.builder(Component.literal("Unassign"), button -> {
					ClientPlayNetworking.send(new UnassignAllRequesterGolemPayload(this.publicId));
					this.minecraft.gui.setScreen(null);
				}).bounds(center - 32, this.height / 2 + 22, 66, 20).build());
			}
			this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.minecraft.gui.setScreen(null))
					.bounds(this.global ? center + 4 : center + 38, this.height / 2 + 22, this.global ? 96 : 62, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.minecraft.gui.setScreen(null))
					.bounds(center - 48, this.height / 2 + 22, 96, 20).build());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int left = this.width / 2 - 128;
		int top = this.height / 2 - 98;
		graphics.fill(left, top, left + 256, top + 196, 0xFF000000);
		graphics.fill(left + 1, top + 1, left + 255, top + 195, 0xD0202020);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 76, 0xFFFFFFFF);
		graphics.centeredText(this.font,
				Component.literal("Public ID: #" + String.format(java.util.Locale.ROOT, "%06d", this.publicId)),
				this.width / 2, this.height / 2 - 54, 0xFFE0E0E0);
		graphics.centeredText(this.font, Component.literal("Status: " + (this.global ? "Global" : "Assigned")),
				this.width / 2, this.height / 2 - 31, this.global ? 0xFF8FE38F : 0xFFFFD479);
		if (!this.canEdit) {
			graphics.centeredText(this.font, Component.literal(this.currentName),
					this.width / 2, this.height / 2 - 4, 0xFFE0E0E0);
		}
		if (this.canClaim) {
			graphics.centeredText(this.font, Component.literal("Sneak + empty-hand right-click to claim"),
					this.width / 2, this.height / 2 + 12, 0xFFB0B0B0);
		} else if (!this.canEdit) {
			graphics.centeredText(this.font, Component.literal("Only the owner can rename or unassign"),
					this.width / 2, this.height / 2 + 12, 0xFFB0B0B0);
		}
		graphics.centeredText(this.font, Component.literal("Upgrade slots — planned for V1.2"),
				this.width / 2, this.height / 2 + 48, 0xFFB0B0B0);
		for (int slot = 0; slot < 3; slot++) {
			int x = this.width / 2 - 27 + slot * 27;
			int y = this.height / 2 + 60;
			graphics.fill(x, y, x + 22, y + 22, 0xFF373737);
			graphics.fill(x + 1, y + 1, x + 21, y + 21, 0xFF8B8B8B);
			graphics.fill(x + 2, y + 2, x + 20, y + 20, 0xFF373737);
		}
	}
}

package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.network.RenameRequesterGolemPayload;
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
	private EditBox nameField;

	public RequesterGolemScreen(int publicId, String currentName, boolean canEdit, boolean canClaim) {
		super(Component.literal("Requester Golem"));
		this.publicId = publicId;
		this.currentName = currentName == null ? "Requester Golem" : currentName;
		this.canEdit = canEdit;
		this.canClaim = canClaim;
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
			this.addRenderableWidget(Button.builder(Component.literal("Save name"), button -> {
				ClientPlayNetworking.send(new RenameRequesterGolemPayload(this.publicId, this.nameField.getValue()));
				this.minecraft.setScreen(null);
			}).bounds(center - 100, this.height / 2 + 24, 96, 20).build());
		}
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.minecraft.setScreen(null))
				.bounds(center + 4, this.height / 2 + 24, 96, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 72, 0xFFFFFFFF);
		graphics.centeredText(this.font,
				Component.literal("Public ID: #" + String.format(java.util.Locale.ROOT, "%06d", this.publicId)),
				this.width / 2, this.height / 2 - 48, 0xFFE0E0E0);
		if (!this.canEdit) {
			String status = this.canClaim
					? "Unowned legacy golem — sneak + empty-hand right-click to claim"
					: "Only the owner can change this golem's name";
			graphics.centeredText(this.font, Component.literal(status),
					this.width / 2, this.height / 2 - 16, 0xFFB0B0B0);
		}
	}
}

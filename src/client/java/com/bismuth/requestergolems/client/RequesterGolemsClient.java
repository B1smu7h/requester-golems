package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.ModMenuTypes;
import com.bismuth.requestergolems.network.OpenRequesterGolemScreenPayload;
import com.bismuth.requestergolems.network.RequesterAssignmentsListPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RequesterGolemsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.REQUESTER_CHEST, RequesterChestScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(RequesterAssignmentsListPayload.TYPE, (payload, context) ->
				context.client().execute(() -> {
					if (context.client().screen instanceof RequesterChestScreen screen) {
						screen.requestergolems$setAssignedGolemsText(payload.assignedGolems());
					}
				}));
		ClientPlayNetworking.registerGlobalReceiver(OpenRequesterGolemScreenPayload.TYPE, (payload, context) ->
				context.client().execute(() -> context.client().gui.setScreen(new RequesterGolemScreen(
						payload.publicId(), payload.displayName(), payload.canEdit(), payload.canClaim(), payload.global()
				))));
	}
}

package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.ModMenuTypes;
import com.bismuth.requestergolems.network.OpenRequesterGolemScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RequesterGolemsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.REQUESTER_CHEST, RequesterChestScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(OpenRequesterGolemScreenPayload.TYPE, (payload, context) ->
				context.client().execute(() -> context.client().gui.setScreen(new RequesterGolemScreen(
						payload.publicId(), payload.displayName(), payload.canEdit(), payload.canClaim()
				))));
	}
}

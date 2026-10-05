package com.bismuth.requestergolems.client;

import com.bismuth.requestergolems.menu.ModMenuTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RequesterGolemsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.REQUESTER_CHEST, RequesterChestScreen::new);
	}
}

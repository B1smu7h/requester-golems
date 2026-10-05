package com.bismuth.requestergolems;

import net.fabricmc.api.ModInitializer;
import com.bismuth.requestergolems.menu.ModMenuTypes;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequesterGolems implements ModInitializer {
	public static final String MOD_ID = "requestergolems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModMenuTypes.REQUESTER_CHEST.getClass();
		LOGGER.info("Requester Golems initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

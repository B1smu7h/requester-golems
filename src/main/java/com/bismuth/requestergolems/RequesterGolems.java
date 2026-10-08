package com.bismuth.requestergolems;

import net.fabricmc.api.ModInitializer;
import com.bismuth.requestergolems.menu.ModMenuTypes;
import com.bismuth.requestergolems.network.CancelRequesterRequestPayload;
import com.bismuth.requestergolems.network.SetRequesterRequestPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequesterGolems implements ModInitializer {
	public static final String MOD_ID = "requestergolems";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModMenuTypes.REQUESTER_CHEST.getClass();
		PayloadTypeRegistry.serverboundPlay().register(
				CancelRequesterRequestPayload.TYPE,
				CancelRequesterRequestPayload.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
				SetRequesterRequestPayload.TYPE,
				SetRequesterRequestPayload.CODEC
		);
		CancelRequesterRequestPayload.registerServerReceiver();
		SetRequesterRequestPayload.registerServerReceiver();
		LOGGER.info("Requester Golems initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

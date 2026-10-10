package com.bismuth.requestergolems;

import net.fabricmc.api.ModInitializer;
import com.bismuth.requestergolems.menu.ModMenuTypes;
import com.bismuth.requestergolems.network.CancelRequesterRequestPayload;
import com.bismuth.requestergolems.network.SetRequesterRequestPayload;
import com.bismuth.requestergolems.network.SetRequesterAssignmentPayload;
import com.bismuth.requestergolems.network.OpenRequesterGolemScreenPayload;
import com.bismuth.requestergolems.network.RenameRequesterGolemPayload;
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
		PayloadTypeRegistry.clientboundPlay().register(
				OpenRequesterGolemScreenPayload.TYPE,
				OpenRequesterGolemScreenPayload.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
				RenameRequesterGolemPayload.TYPE,
				RenameRequesterGolemPayload.CODEC
		);
		PayloadTypeRegistry.serverboundPlay().register(
				SetRequesterAssignmentPayload.TYPE,
				SetRequesterAssignmentPayload.CODEC
		);
		CancelRequesterRequestPayload.registerServerReceiver();
		SetRequesterRequestPayload.registerServerReceiver();
		SetRequesterAssignmentPayload.registerServerReceiver();
		RenameRequesterGolemPayload.registerServerReceiver();
		LOGGER.info("Requester Golems initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

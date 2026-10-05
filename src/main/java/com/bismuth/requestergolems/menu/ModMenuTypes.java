package com.bismuth.requestergolems.menu;

import com.bismuth.requestergolems.RequesterGolems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlagSet;

public final class ModMenuTypes {
	public static final MenuType<RequesterChestMenu> REQUESTER_CHEST =
			Registry.register(
					BuiltInRegistries.MENU,
					RequesterGolems.id("requester_chest"),
					new MenuType<>(RequesterChestMenu::new, FeatureFlagSet.of())
			);

	private ModMenuTypes() {
	}
}

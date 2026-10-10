package com.bismuth.requestergolems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/**
 * Searches only entities already present in loaded server levels. It never loads
 * chunks or keeps a second, potentially stale entity directory.
 */
public final class RequesterGolemLookup {
	private RequesterGolemLookup() {
	}

	public static List<CopperGolem> findLoaded(MinecraftServer server, String query, int limit) {
		String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		if (needle.isEmpty()) return List.of();

		boolean numeric = needle.startsWith("#") ? needle.substring(1).matches("\\d{1,10}") : needle.matches("\\d{1,10}");
		int id = -1;
		if (numeric) {
			try {
				id = Integer.parseInt(needle.startsWith("#") ? needle.substring(1) : needle);
			} catch (NumberFormatException ignored) {
				numeric = false;
			}
		}

		List<CopperGolem> matches = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (!(entity instanceof CopperGolem golem)
						|| !(golem instanceof RequesterGolemAccess access)
						|| !access.requestergolems$isRequester()) continue;

				String name = access.requestergolems$getDisplayName().toLowerCase(Locale.ROOT);
				boolean match = numeric ? access.requestergolems$getPublicId() == id : name.contains(needle);
				if (match) matches.add(golem);
			}
		}
		matches.sort(Comparator.comparingInt(g -> ((RequesterGolemAccess) g).requestergolems$getPublicId()));
		if (matches.size() > limit) return List.copyOf(matches.subList(0, limit));
		return List.copyOf(matches);
	}
}

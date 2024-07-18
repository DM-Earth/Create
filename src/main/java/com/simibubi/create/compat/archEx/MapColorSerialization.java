package com.simibubi.create.compat.archEx;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.MapColor;

// https://github.com/DebuggyTeam/architecture-extensions/blob/1.20/src/main/java/io/github/debuggyteam/architecture_extensions/util/MapColors.java
public class MapColorSerialization {
	private static final Map<MapColor, String> names = new HashMap<>();

	static {
		// there's too many, so this is the bare minimum
		names.put(MapColor.BLUE, "blue");
		names.put(MapColor.RED, "red");
		names.put(MapColor.PALE_YELLOW, "sand");
		names.put(MapColor.TERRACOTTA_YELLOW, "yellow_terracotta");
		names.put(MapColor.BROWN, "brown");
		names.put(MapColor.TERRACOTTA_GRAY, "gray_terracotta");
		names.put(MapColor.TEAL, "warped_nylium");
		names.put(MapColor.DIRT_BROWN, "dirt");
		names.put(MapColor.OFF_WHITE, "quartz");
		names.put(MapColor.STONE_GRAY, "stone");
		names.put(MapColor.TERRACOTTA_WHITE, "white_terracotta");
		names.put(MapColor.TERRACOTTA_BROWN, "brown_terracotta");
		names.put(MapColor.DEEPSLATE_GRAY, "deepslate");
	}

	public static String getArchExName(MapColor color) {
		String name = names.get(color);
		if (name == null)
			throw new IllegalArgumentException("Unsupported MapColor: " + color);
		return name;
	}
}

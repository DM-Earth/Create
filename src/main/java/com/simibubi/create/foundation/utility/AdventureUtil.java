package com.simibubi.create.foundation.utility;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public class AdventureUtil {
	public static boolean isAdventure(@Nullable PlayerEntity player) {
		return player != null && !player.canModifyBlocks() && !player.isSpectator();
	}
}

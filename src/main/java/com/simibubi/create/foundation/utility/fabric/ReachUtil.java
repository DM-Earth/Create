package com.simibubi.create.foundation.utility.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

public class ReachUtil {
	public static double reach(PlayerEntity p) {
		return ReachEntityAttributes.getReachDistance(p, p.isCreative() ? 5 : 4.5);
	}
}

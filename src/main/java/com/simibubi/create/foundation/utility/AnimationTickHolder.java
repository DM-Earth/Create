package com.simibubi.create.foundation.utility;

import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedClientWorld;

import io.github.fabricators_of_create.porting_lib.common.util.MinecraftClientUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.WorldAccess;

public class AnimationTickHolder {

	private static int ticks;
	private static int pausedTicks;

	public static void reset() {
		ticks = 0;
		pausedTicks = 0;
	}

	public static void tick() {
		if (!MinecraftClient.getInstance()
			.isPaused()) {
			ticks = (ticks + 1) % 1_728_000; // wrap around every 24 hours so we maintain enough floating point precision
		} else {
			pausedTicks = (pausedTicks + 1) % 1_728_000;
		}
	}

	public static int getTicks() {
		return getTicks(false);
	}

	public static int getTicks(boolean includePaused) {
		return includePaused ? ticks + pausedTicks : ticks;
	}

	public static float getRenderTime() {
		return getTicks() + getPartialTicks();
	}

	public static float getPartialTicks() {
		MinecraftClient mc = MinecraftClient.getInstance();
		return (mc.isPaused() ? MinecraftClientUtil.getRenderPartialTicksPaused(mc) : mc.getTickDelta());
	}

	public static int getTicks(WorldAccess world) {
		if (world instanceof WrappedClientWorld)
			return getTicks(((WrappedClientWorld) world).getWrappedWorld());
		return world instanceof PonderWorld ? PonderUI.ponderTicks : getTicks();
	}

	public static float getRenderTime(WorldAccess world) {
		return getTicks(world) + getPartialTicks(world);
	}

	public static float getPartialTicks(WorldAccess world) {
		return world instanceof PonderWorld ? PonderUI.getPartialTicks() : getPartialTicks();
	}
}

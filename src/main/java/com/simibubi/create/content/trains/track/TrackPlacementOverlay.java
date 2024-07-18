package com.simibubi.create.content.trains.track;

import com.simibubi.create.foundation.mixin.fabric.GuiAccessor;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

public class TrackPlacementOverlay {

	@Environment(EnvType.CLIENT)
	public static void renderOverlay(InGameHud gui, DrawContext graphics) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;
		if (TrackPlacement.hoveringPos == null)
			return;
		if (TrackPlacement.cached == null || TrackPlacement.cached.curve == null || !TrackPlacement.cached.valid)
			return;
		if (TrackPlacement.extraTipWarmup < 4)
			return;

		if (((GuiAccessor) gui).getHeldItemTooltipFade() > 0)
			return;

		boolean active = mc.options.sprintKey.isPressed();
		MutableText text = Lang.translateDirect("track.hold_for_smooth_curve", Components.keybind("key.sprint")
			.formatted(active ? Formatting.WHITE : Formatting.GRAY));

		Window window = mc.getWindow();
		int x = (window.getScaledWidth() - gui.getTextRenderer()
			.getWidth(text)) / 2;
		int y = window.getScaledHeight() - 61;
		Color color = new Color(0x4ADB4A).setAlpha(MathHelper.clamp((TrackPlacement.extraTipWarmup - 4) / 3f, 0.1f, 1));
		graphics.drawText(gui.getTextRenderer(), text, x, y, color.getRGB(), false);
	}

}

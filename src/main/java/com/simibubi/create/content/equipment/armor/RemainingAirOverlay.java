package com.simibubi.create.content.equipment.armor;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import net.minecraft.world.GameMode;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;

public class RemainingAirOverlay {
	public static void render(DrawContext graphics, int width, int height) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;

		ClientPlayerEntity player = mc.player;
		if (player == null)
			return;
		if (player.isCreative())
			return;
		if (!player.getCustomData()
			.contains("VisualBacktankAir"))
			return;
		if (!player.isSubmergedIn(FluidTags.WATER) && !player.isInLava())
			return;

		int timeLeft = player.getCustomData()
			.getInt("VisualBacktankAir");

		MatrixStack poseStack = graphics.getMatrices();
		poseStack.push();

		ItemStack backtank = getDisplayedBacktank(player);
		poseStack.translate(width / 2 + 90, height - 53 + (backtank.getItem()
			.isFireproof() ? 9 : 0), 0);

		Text text = Components.literal(StringHelper.formatTicks(Math.max(0, timeLeft - 1) * 20));
		GuiGameElement.of(backtank)
			.at(0, 0)
			.render(graphics);
		int color = 0xFF_FFFFFF;
		if (timeLeft < 60 && timeLeft % 2 == 0) {
			color = Color.mixColors(0xFF_FF0000, color, Math.max(timeLeft / 60f, .25f));
		}
		graphics.drawTextWithShadow(mc.textRenderer, text, 16, 5, color);

		poseStack.pop();
	}

	public static ItemStack getDisplayedBacktank(ClientPlayerEntity player) {
		List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
		if (!backtanks.isEmpty()) {
			return backtanks.get(0);
		}
		return AllItems.COPPER_BACKTANK.asStack();
	}
}

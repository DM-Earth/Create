package com.simibubi.create.content.equipment.goggles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.outliner.Outline;
import com.simibubi.create.foundation.outliner.Outliner.OutlineEntry;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import io.github.fabricators_of_create.porting_lib.util.client.ScreenUtils;

public class GoggleOverlayRenderer {

	private static final Map<Object, OutlineEntry> outlines = CreateClient.OUTLINER.getOutlines();

	public static int hoverTicks = 0;
	public static BlockPos lastHovered = null;

	public static void renderOverlay(DrawContext graphics, float partialTicks, Window window) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;

		HitResult objectMouseOver = mc.crosshairTarget;
		if (!(objectMouseOver instanceof BlockHitResult)) {
			lastHovered = null;
			hoverTicks = 0;
			return;
		}

		for (OutlineEntry entry : outlines.values()) {
			if (!entry.isAlive())
				continue;
			Outline outline = entry.getOutline();
			if (outline instanceof ValueBox && !((ValueBox) outline).isPassive)
				return;
		}

		BlockHitResult result = (BlockHitResult) objectMouseOver;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();

		int prevHoverTicks = hoverTicks;
		hoverTicks++;
		lastHovered = pos;

		pos = proxiedOverlayPosition(world, pos);

		BlockEntity be = world.getBlockEntity(pos);
		boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);

		boolean hasGoggleInformation = be instanceof IHaveGoggleInformation;
		boolean hasHoveringInformation = be instanceof IHaveHoveringInformation;

		boolean goggleAddedInformation = false;
		boolean hoverAddedInformation = false;

		List<Text> tooltip = new ArrayList<>();

		if (hasGoggleInformation && wearingGoggles) {
			IHaveGoggleInformation gte = (IHaveGoggleInformation) be;
			goggleAddedInformation = gte.addToGoggleTooltip(tooltip, mc.player.isSneaking());
		}

		if (hasHoveringInformation) {
			if (!tooltip.isEmpty())
				tooltip.add(Components.immutableEmpty());
			IHaveHoveringInformation hte = (IHaveHoveringInformation) be;
			hoverAddedInformation = hte.addToTooltip(tooltip, mc.player.isSneaking());

			if (goggleAddedInformation && !hoverAddedInformation)
				tooltip.remove(tooltip.size() - 1);
		}

		if (be instanceof IDisplayAssemblyExceptions) {
			boolean exceptionAdded = ((IDisplayAssemblyExceptions) be).addExceptionToTooltip(tooltip);
			if (exceptionAdded) {
				hasHoveringInformation = true;
				hoverAddedInformation = true;
			}
		}

		if (!hasHoveringInformation)
			if (hasHoveringInformation =
				hoverAddedInformation = TrainRelocator.addToTooltip(tooltip, mc.player.isSneaking()))
				hoverTicks = prevHoverTicks + 1;

		// break early if goggle or hover returned false when present
		if ((hasGoggleInformation && !goggleAddedInformation) && (hasHoveringInformation && !hoverAddedInformation)) {
			hoverTicks = 0;
			return;
		}

		// check for piston poles if goggles are worn
		BlockState state = world.getBlockState(pos);
		if (wearingGoggles && AllBlocks.PISTON_EXTENSION_POLE.has(state)) {
			Direction[] directions = Iterate.directionsInAxis(state.get(PistonExtensionPoleBlock.FACING)
				.getAxis());
			int poles = 1;
			boolean pistonFound = false;
			for (Direction dir : directions) {
				int attachedPoles = PistonExtensionPoleBlock.PlacementHelper.get()
					.attachedPoles(world, pos, dir);
				poles += attachedPoles;
				pistonFound |= world.getBlockState(pos.offset(dir, attachedPoles + 1))
					.getBlock() instanceof MechanicalPistonBlock;
			}

			if (!pistonFound) {
				hoverTicks = 0;
				return;
			}
			if (!tooltip.isEmpty())
				tooltip.add(Components.immutableEmpty());

			tooltip.add(IHaveGoggleInformation.componentSpacing.copyContentOnly()
				.append(Lang.translateDirect("gui.goggles.pole_length"))
				.append(Components.literal(" " + poles)));
		}

		if (tooltip.isEmpty()) {
			hoverTicks = 0;
			return;
		}

		MatrixStack poseStack = graphics.getMatrices();
		poseStack.push();

		int tooltipTextWidth = 0;
		for (StringVisitable textLine : tooltip) {
			int textLineWidth = mc.textRenderer.getWidth(textLine);
			if (textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}

		int tooltipHeight = 8;
		if (tooltip.size() > 1) {
			tooltipHeight += 2; // gap between title lines and next lines
			tooltipHeight += (tooltip.size() - 1) * 10;
		}

		CClient cfg = AllConfigs.client();
		int posX = mc.getWindow().getScaledWidth() / 2 + cfg.overlayOffsetX.get();
		int posY = mc.getWindow().getScaledHeight() / 2 + cfg.overlayOffsetY.get();

		posX = Math.min(posX, mc.getWindow().getScaledWidth() - tooltipTextWidth - 20);
		posY = Math.min(posY, mc.getWindow().getScaledHeight() - tooltipHeight - 20);

		float fade = MathHelper.clamp((hoverTicks + partialTicks) / 24f, 0, 1);
		Boolean useCustom = cfg.overlayCustomColor.get();
		Color colorBackground = useCustom ? new Color(cfg.overlayBackgroundColor.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BACKGROUND)
				.scaleAlpha(.75f);
		Color colorBorderTop = useCustom ? new Color(cfg.overlayBorderColorTop.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, true)
				.copy();
		Color colorBorderBot = useCustom ? new Color(cfg.overlayBorderColorBot.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, false)
				.copy();

		if (fade < 1) {
			poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
			colorBackground.scaleAlpha(fade);
			colorBorderTop.scaleAlpha(fade);
			colorBorderBot.scaleAlpha(fade);
		}

		RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), -1, colorBackground.getRGB(),
			colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.textRenderer);

		ItemStack item = AllItems.GOGGLES.asStack();
		GuiGameElement.of(item)
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		poseStack.pop();
	}

	public static BlockPos proxiedOverlayPosition(World level, BlockPos pos) {
		BlockState targetedState = level.getBlockState(pos);
		if (targetedState.getBlock() instanceof IProxyHoveringInformation proxy)
			return proxy.getInformationSource(level, pos, targetedState);
		return pos;
	}

}

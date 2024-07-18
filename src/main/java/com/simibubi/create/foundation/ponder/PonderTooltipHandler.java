package com.simibubi.create.foundation.ponder;

import java.util.List;

import com.google.common.base.Strings;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.ponder.ui.NavigatableSimiScreen;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import io.github.fabricators_of_create.porting_lib.event.client.RenderTooltipBorderColorCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PonderTooltipHandler {

	public static boolean enable = true;

	static LerpedFloat holdWProgress = LerpedFloat.linear()
		.startWithValue(0);
	static ItemStack hoveredStack = ItemStack.EMPTY;
	static ItemStack trackingStack = ItemStack.EMPTY;
	static boolean subject = false;
	static boolean deferTick = false;

	public static final String HOLD_TO_PONDER = PonderLocalization.LANG_PREFIX + "hold_to_ponder";
	public static final String SUBJECT = PonderLocalization.LANG_PREFIX + "subject";

	public static void tick() {
		deferTick = true;
	}

	public static void deferredTick() {
		deferTick = false;
		MinecraftClient instance = MinecraftClient.getInstance();
		Screen currentScreen = instance.currentScreen;

		if (hoveredStack.isEmpty() || trackingStack.isEmpty()) {
			trackingStack = ItemStack.EMPTY;
			holdWProgress.startWithValue(0);
			return;
		}

		float value = holdWProgress.getValue();
		int keyCode = KeyBindingHelper.getBoundKeyOf(ponderKeybind()).getCode();
		long window = instance.getWindow()
			.getHandle();

		if (!subject && InputUtil.isKeyPressed(window, keyCode)) {
			if (value >= 1) {
				if (currentScreen instanceof NavigatableSimiScreen)
					((NavigatableSimiScreen) currentScreen).centerScalingOnMouse();
				ScreenOpener.transitionTo(PonderUI.of(trackingStack));
				holdWProgress.startWithValue(0);
				return;
			}
			holdWProgress.setValue(Math.min(1, value + Math.max(.25f, value) * .25f));
		} else
			holdWProgress.setValue(Math.max(0, value - .05f));

		hoveredStack = ItemStack.EMPTY;
	}

	public static void addToTooltip(ItemStack stack, List<Text> tooltip) {
		if (!enable)
			return;

		updateHovered(stack);

		if (deferTick)
			deferredTick();

		if (trackingStack != stack)
			return;

		float renderPartialTicks = MinecraftClient.getInstance()
			.getTickDelta();
		Text component = subject ? Lang.translateDirect(SUBJECT)
			.formatted(Formatting.GREEN)
			: makeProgressBar(Math.min(1, holdWProgress.getValue(renderPartialTicks) * 8 / 7f));
		if (tooltip.size() < 2)
			tooltip.add(component);
		else
			tooltip.add(1, component);
	}

	protected static void updateHovered(ItemStack stack) {
		MinecraftClient instance = MinecraftClient.getInstance();
		Screen currentScreen = instance.currentScreen;
		boolean inPonderUI = currentScreen instanceof PonderUI;

		ItemStack prevStack = trackingStack;
		hoveredStack = ItemStack.EMPTY;
		subject = false;

		if (inPonderUI) {
			PonderUI ponderUI = (PonderUI) currentScreen;
			if (ItemHelper.sameItem(stack, ponderUI.getSubject()))
				subject = true;
		}

		if (stack.isEmpty())
			return;
		if (!PonderRegistry.ALL.containsKey(RegisteredObjects.getKeyOrThrow(stack.getItem())))
			return;

		if (prevStack.isEmpty() || !ItemHelper.sameItem(prevStack, stack))
			holdWProgress.startWithValue(0);

		hoveredStack = stack;
		trackingStack = stack;
	}

	public static RenderTooltipBorderColorCallback.BorderColorEntry handleTooltipColor(ItemStack stack, int originalBorderColorStart, int originalBorderColorEn) {
		if (trackingStack != stack)
			return null;
		if (holdWProgress.getValue() == 0)
			return null;
		float renderPartialTicks = MinecraftClient.getInstance()
			.getTickDelta();
		int start = originalBorderColorStart;
		int end = originalBorderColorEn;
		float progress = Math.min(1, holdWProgress.getValue(renderPartialTicks) * 8 / 7f);

		start = getSmoothColorForProgress(progress);
		end = getSmoothColorForProgress((progress));

		return new RenderTooltipBorderColorCallback.BorderColorEntry(start | 0xa0000000, end | 0xa0000000);
	}

	private static int getSmoothColorForProgress(float progress) {
		if (progress < .5f)
			return Color.mixColors(0x5000FF, 5592575, progress * 2);
		return Color.mixColors(5592575, 0xffffff, (progress - .5f) * 2);
	}

	private static Text makeProgressBar(float progress) {
		MutableText holdW = Lang
			.translateDirect(HOLD_TO_PONDER,
				((MutableText) ponderKeybind().getBoundKeyLocalizedText()).formatted(Formatting.GRAY))
			.formatted(Formatting.DARK_GRAY);

		TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
		float charWidth = fontRenderer.getWidth("|");
		float tipWidth = fontRenderer.getWidth(holdW);

		int total = (int) (tipWidth / charWidth);
		int current = (int) (progress * total);

		if (progress > 0) {
			String bars = "";
			bars += Formatting.GRAY + Strings.repeat("|", current);
			if (progress < 1)
				bars += Formatting.DARK_GRAY + Strings.repeat("|", total - current);
			return Components.literal(bars);
		}

		return holdW;
	}

	protected static KeyBinding ponderKeybind() {
		return MinecraftClient.getInstance().options.forwardKey;
	}

}

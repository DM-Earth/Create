package com.simibubi.create.content.logistics.filter;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;

import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import io.github.fabricators_of_create.porting_lib.util.PlayerEntityHelper;

public abstract class AbstractFilterScreen<F extends AbstractFilterMenu> extends AbstractSimiContainerScreen<F> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	protected AbstractFilterScreen(F menu, PlayerInventory inv, Text title, AllGuiTextures background) {
		super(menu, inv, title);
		this.background = background;
	}

	@Override
	protected void init() {
		setWindowSize(Math.max(background.width, PLAYER_INVENTORY.width),
			background.height + 4 + PLAYER_INVENTORY.height);
		super.init();

		int screenX = x;
		int screenY = y;

		resetButton = new IconButton(screenX + background.width - 62, screenY + background.height - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			handler.clearContents();
			contentsCleared();
			handler.sendClearPacket();
		});
		confirmButton = new IconButton(screenX + background.width - 33, screenY + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			client.player.closeHandledScreen();
		});

		addDrawableChild(resetButton);
		addDrawableChild(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + background.width, y + background.height - 40, 80, 48));
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.width);
		int invY = y + background.height + 4;
		renderPlayerInventory(graphics, invX, invY);

		int screenX = x;
		int screenY = y;

		background.render(graphics, x, y);
		graphics.drawText(textRenderer, title, screenX + (background.width - 8) / 2 - textRenderer.getWidth(title) / 2, screenY + 4,
			AllItems.FILTER.isIn(handler.contentHolder) ? 0x303030 : 0x592424, false);

		GuiGameElement.of(handler.contentHolder).<GuiGameElement
			.GuiRenderBuilder>at(x + background.width + 8, y + background.height - 52, -200)
			.scale(4)
			.render(graphics);
	}

	@Override
	protected void handledScreenTick() {
		if (!ItemStack.areEqual(handler.player.getMainHandStack(), handler.contentHolder))
			PlayerEntityHelper.closeScreen(handler.player);

		super.handledScreenTick();

		handleTooltips();
		handleIndicators();
	}

	protected void handleTooltips() {
		List<IconButton> tooltipButtons = getTooltipButtons();

		for (IconButton button : tooltipButtons) {
			if (!button.getToolTip()
				.isEmpty()) {
				button.setToolTip(button.getToolTip()
					.get(0));
				button.getToolTip()
					.add(TooltipHelper.holdShift(Palette.YELLOW, hasShiftDown()));
			}
		}

		if (hasShiftDown()) {
			List<MutableText> tooltipDescriptions = getTooltipDescriptions();
			for (int i = 0; i < tooltipButtons.size(); i++)
				fillToolTip(tooltipButtons.get(i), tooltipDescriptions.get(i));
		}
	}

	public void handleIndicators() {
		for (IconButton button : getTooltipButtons())
			button.active = isButtonEnabled(button);
		for (Indicator indicator : getIndicators())
			indicator.state = isIndicatorOn(indicator) ? State.ON : State.OFF;
	}

	protected abstract boolean isButtonEnabled(IconButton button);

	protected abstract boolean isIndicatorOn(Indicator indicator);

	protected List<IconButton> getTooltipButtons() {
		return Collections.emptyList();
	}

	protected List<MutableText> getTooltipDescriptions() {
		return Collections.emptyList();
	}

	protected List<Indicator> getIndicators() {
		return Collections.emptyList();
	}

	private void fillToolTip(IconButton button, Text tooltip) {
		if (!button.isSelected())
			return;
		List<Text> tip = button.getToolTip();
		tip.addAll(TooltipHelper.cutTextComponent(tooltip, Palette.ALL_GRAY));
	}

	protected void contentsCleared() {}

	protected void sendOptionUpdate(Option option) {
		AllPackets.getChannel()
			.sendToServer(new FilterScreenPacket(option));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}

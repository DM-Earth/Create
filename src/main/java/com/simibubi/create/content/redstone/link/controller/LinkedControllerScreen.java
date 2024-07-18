package com.simibubi.create.content.redstone.link.controller;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.SlotAccessor;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import io.github.fabricators_of_create.porting_lib.util.PlayerEntityHelper;

public class LinkedControllerScreen extends AbstractSimiContainerScreen<LinkedControllerMenu> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public LinkedControllerScreen(LinkedControllerMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title);
		this.background = AllGuiTextures.LINKED_CONTROLLER;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height + 4 + PLAYER_INVENTORY.height);
		setWindowOffset(1, 0);
		super.init();

		int screenX = x;
		int screenY = y;

		resetButton = new IconButton(screenX + background.width - 62, screenY + background.height - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			handler.clearContents();
			handler.sendClearPacket();
		});
		confirmButton = new IconButton(screenX + background.width - 33, screenY + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			client.player.closeHandledScreen();
		});

		addDrawableChild(resetButton);
		addDrawableChild(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(screenX + background.width + 4, screenY + background.height - 44, 64, 56));
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.width);
		int invY = y + background.height + 4;
		renderPlayerInventory(graphics, invX, invY);

		int screenX = x;
		int screenY = y;

		background.render(graphics, screenX, screenY);
		graphics.drawText(textRenderer, title, screenX + 15, screenY + 4, 0x592424, false);

		GuiGameElement.of(handler.contentHolder).<GuiGameElement
			.GuiRenderBuilder>at(screenX + background.width - 4, screenY + background.height - 56, -200)
			.scale(5)
			.render(graphics);
	}

	@Override
	protected void handledScreenTick() {
		if (!ItemStack.areEqual(handler.player.getMainHandStack(),handler.contentHolder))
			PlayerEntityHelper.closeScreen(handler.player);

		super.handledScreenTick();
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext graphics, int x, int y) {
		if (!handler.getCursorStack()
			.isEmpty() || this.focusedSlot == null || focusedSlot.inventory == handler.playerInventory) {
			super.drawMouseoverTooltip(graphics, x, y);
			return;
		}

		List<Text> list = new LinkedList<>();
		if (focusedSlot.hasStack())
			list = getTooltipFromItem(focusedSlot.getStack());

		graphics.drawTooltip(textRenderer, addToTooltip(list, ((SlotAccessor)focusedSlot).port_lib$getSlotIndex()), x, y);
	}

	private List<Text> addToTooltip(List<Text> list, int slot) {
		if (slot < 0 || slot >= 12)
			return list;
		list.add(Lang.translateDirect("linked_controller.frequency_slot_" + ((slot % 2) + 1), ControlsUtil.getControls()
			.get(slot / 2)
			.getBoundKeyLocalizedText()
			.getString())
			.formatted(Formatting.GOLD));
		return list;
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}

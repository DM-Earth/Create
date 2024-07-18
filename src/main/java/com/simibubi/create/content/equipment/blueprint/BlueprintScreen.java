package com.simibubi.create.content.equipment.blueprint;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PlayerAccessor;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.SlotAccessor;

public class BlueprintScreen extends AbstractSimiContainerScreen<BlueprintMenu> {

	protected AllGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public BlueprintScreen(BlueprintMenu menu, PlayerInventory inv, Text title) {
		super(menu, inv, title);
		this.background = AllGuiTextures.BLUEPRINT;
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
			contentsCleared();
			handler.sendClearPacket();
		});
		confirmButton = new IconButton(screenX + background.width - 33, screenY + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			client.player.closeHandledScreen();
		});

		addDrawableChild(resetButton);
		addDrawableChild(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(screenX + background.width, screenY + background.height - 36, 56, 44));
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(PLAYER_INVENTORY.width);
		int invY = y + background.height + 4;
		renderPlayerInventory(graphics, invX, invY);

		int screenX = x;
		int screenY = y;

		background.render(graphics, screenX, screenY);
		graphics.drawText(textRenderer, title, screenX + 15, screenY + 4, 0xFFFFFF, false);

		GuiGameElement.of(AllPartialModels.CRAFTING_BLUEPRINT_1x1).<GuiGameElement
			.GuiRenderBuilder>at(screenX + background.width + 20, screenY + background.height - 32, 0)
			.rotate(45, -45, 22.5f)
			.scale(40)
			.render(graphics);
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

		graphics.drawTooltip(textRenderer, addToTooltip(list, ((SlotAccessor) focusedSlot).port_lib$getSlotIndex(), true), x, y);
	}

	private List<Text> addToTooltip(List<Text> list, int slot, boolean isEmptySlot) {
		if (slot < 0 || slot > 10)
			return list;

		if (slot < 9) {
			list.add(Lang.translateDirect("crafting_blueprint.crafting_slot")
				.formatted(Formatting.GOLD));
			if (isEmptySlot)
				list.add(Lang.translateDirect("crafting_blueprint.filter_items_viable")
					.formatted(Formatting.GRAY));

		} else if (slot == 9) {
			list.add(Lang.translateDirect("crafting_blueprint.display_slot")
				.formatted(Formatting.GOLD));
			if (!isEmptySlot)
				list.add(Lang
					.translateDirect(
						"crafting_blueprint." + (handler.contentHolder.inferredIcon ? "inferred" : "manually_assigned"))
					.formatted(Formatting.GRAY));

		} else if (slot == 10) {
			list.add(Lang.translateDirect("crafting_blueprint.secondary_display_slot")
				.formatted(Formatting.GOLD));
			if (isEmptySlot)
				list.add(Lang.translateDirect("crafting_blueprint.optional")
					.formatted(Formatting.GRAY));
		}

		return list;
	}

	@Override
	protected void handledScreenTick() {
		if (!handler.contentHolder.isEntityAlive())
			((PlayerAccessor) handler.player).port_lib$closeScreen();

		super.handledScreenTick();

//		handleTooltips();
	}

//	protected void handleTooltips() {
//		List<IconButton> tooltipButtons = getTooltipButtons();
//
//		for (IconButton button : tooltipButtons) {
//			if (!button.getToolTip()
//				.isEmpty()) {
//				button.setToolTip(button.getToolTip()
//					.get(0));
//				button.getToolTip()
//					.add(TooltipHelper.holdShift(Palette.Yellow, hasShiftDown()));
//			}
//		}
//
//		if (hasShiftDown()) {
//			List<IFormattableTextComponent> tooltipDescriptions = getTooltipDescriptions();
//			for (int i = 0; i < tooltipButtons.size(); i++)
//				fillToolTip(tooltipButtons.get(i), tooltipDescriptions.get(i));
//		}
//	}

	protected void contentsCleared() {}

	protected void sendOptionUpdate(Option option) {
		AllPackets.getChannel().sendToServer(new FilterScreenPacket(option));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}

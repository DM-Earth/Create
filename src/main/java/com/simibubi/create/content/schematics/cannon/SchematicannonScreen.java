package com.simibubi.create.content.schematics.cannon;

import static net.minecraft.util.Formatting.BLUE;
import static net.minecraft.util.Formatting.DARK_PURPLE;
import static net.minecraft.util.Formatting.GRAY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.schematics.cannon.ConfigureSchematicannonPacket.Option;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public class SchematicannonScreen extends AbstractSimiContainerScreen<SchematicannonMenu> {

	private static final AllGuiTextures BG_BOTTOM = AllGuiTextures.SCHEMATICANNON_BOTTOM;
	private static final AllGuiTextures BG_TOP = AllGuiTextures.SCHEMATICANNON_TOP;

	private final Text listPrinter = Lang.translateDirect("gui.schematicannon.listPrinter");
	private final String _gunpowderLevel = "gui.schematicannon.gunpowderLevel";
	private final String _shotsRemaining = "gui.schematicannon.shotsRemaining";
	private final String _showSettings = "gui.schematicannon.showOptions";
	private final String _shotsRemainingWithBackup = "gui.schematicannon.shotsRemainingWithBackup";

	private final String _slotGunpowder = "gui.schematicannon.slot.gunpowder";
	private final String _slotListPrinter = "gui.schematicannon.slot.listPrinter";
	private final String _slotSchematic = "gui.schematicannon.slot.schematic";

	private final Text optionEnabled = Lang.translateDirect("gui.schematicannon.optionEnabled");
	private final Text optionDisabled = Lang.translateDirect("gui.schematicannon.optionDisabled");

	protected Vector<Indicator> replaceLevelIndicators;
	protected Vector<IconButton> replaceLevelButtons;

	protected IconButton skipMissingButton;
	protected Indicator skipMissingIndicator;
	protected IconButton skipBlockEntitiesButton;
	protected Indicator skipBlockEntitiesIndicator;

	protected IconButton playButton;
	protected Indicator playIndicator;
	protected IconButton pauseButton;
	protected Indicator pauseIndicator;
	protected IconButton resetButton;
	protected Indicator resetIndicator;

	private IconButton confirmButton;
	private IconButton showSettingsButton;
	private Indicator showSettingsIndicator;

	protected List<ClickableWidget> placementSettingWidgets;

	private final ItemStack renderedItem = AllBlocks.SCHEMATICANNON.asStack();

	private List<Rect2i> extraAreas = Collections.emptyList();

	public SchematicannonScreen(SchematicannonMenu menu, PlayerInventory inventory, Text title) {
		super(menu, inventory, title);
		placementSettingWidgets = new ArrayList<>();
	}

	@Override
	protected void init() {
		setWindowSize(BG_TOP.width, BG_TOP.height + BG_BOTTOM.height + 2 + AllGuiTextures.PLAYER_INVENTORY.height);
		setWindowOffset(-11, 0);
		super.init();

//		int x = x;
//		int y = y;

		// Play Pause Stop
		playButton = new IconButton(x + 75, y + 86, AllIcons.I_PLAY);
		playButton.withCallback(() -> {
			sendOptionUpdate(Option.PLAY, true);
		});
		playIndicator = new Indicator(x + 75, y + 79, Components.immutableEmpty());
		pauseButton = new IconButton(x + 93, y + 86, AllIcons.I_PAUSE);
		pauseButton.withCallback(() -> {
			sendOptionUpdate(Option.PAUSE, true);
		});
		pauseIndicator = new Indicator(x + 93, y + 79, Components.immutableEmpty());
		resetButton = new IconButton(x + 111, y + 86, AllIcons.I_STOP);
		resetButton.withCallback(() -> {
			sendOptionUpdate(Option.STOP, true);
		});
		resetIndicator = new Indicator(x + 111, y + 79, Components.immutableEmpty());
		resetIndicator.state = State.RED;
		addRenderableWidgets(playButton, playIndicator, pauseButton, pauseIndicator, resetButton,
			resetIndicator);

		confirmButton = new IconButton(x + 180, y + 117, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			client.player.closeHandledScreen();
		});
		addDrawableChild(confirmButton);
		showSettingsButton = new IconButton(x + 9, y + 117, AllIcons.I_PLACEMENT_SETTINGS);
		showSettingsButton.withCallback(() -> {
			showSettingsIndicator.state = placementSettingsHidden() ? State.GREEN : State.OFF;
			initPlacementSettings();
		});
		showSettingsButton.setToolTip(Lang.translateDirect(_showSettings));
		addDrawableChild(showSettingsButton);
		showSettingsIndicator = new Indicator(x + 9, y + 111, Components.immutableEmpty());
		addDrawableChild(showSettingsIndicator);

		extraAreas = ImmutableList.of(new Rect2i(x + BG_TOP.width, y + BG_TOP.height + BG_BOTTOM.height - 62, 84, 92));

		tick();
	}

	private void initPlacementSettings() {
		removeWidgets(placementSettingWidgets);
		placementSettingWidgets.clear();

		if (placementSettingsHidden())
			return;

//		int x = x;
//		int y = y;

		// Replace settings
		replaceLevelButtons = new Vector<>(4);
		replaceLevelIndicators = new Vector<>(4);
		List<AllIcons> icons = ImmutableList.of(AllIcons.I_DONT_REPLACE, AllIcons.I_REPLACE_SOLID,
			AllIcons.I_REPLACE_ANY, AllIcons.I_REPLACE_EMPTY);
		List<Text> toolTips = ImmutableList.of(Lang.translateDirect("gui.schematicannon.option.dontReplaceSolid"),
			Lang.translateDirect("gui.schematicannon.option.replaceWithSolid"),
			Lang.translateDirect("gui.schematicannon.option.replaceWithAny"),
			Lang.translateDirect("gui.schematicannon.option.replaceWithEmpty"));

		for (int i = 0; i < 4; i++) {
			replaceLevelIndicators.add(new Indicator(x + 33 + i * 18, y + 111, Components.immutableEmpty()));
			IconButton replaceLevelButton = new IconButton(x + 33 + i * 18, y + 117, icons.get(i));
			int replaceMode = i;
			replaceLevelButton.withCallback(() -> {
				if (handler.contentHolder.replaceMode != replaceMode)
					sendOptionUpdate(Option.values()[replaceMode], true);
			});
			replaceLevelButton.setToolTip(toolTips.get(i));
			replaceLevelButtons.add(replaceLevelButton);
		}
		placementSettingWidgets.addAll(replaceLevelButtons);
		placementSettingWidgets.addAll(replaceLevelIndicators);

		// Other Settings
		skipMissingButton = new IconButton(x + 111, y + 117, AllIcons.I_SKIP_MISSING);
		skipMissingButton.withCallback(() -> {
			sendOptionUpdate(Option.SKIP_MISSING, !handler.contentHolder.skipMissing);
		});
		skipMissingButton.setToolTip(Lang.translateDirect("gui.schematicannon.option.skipMissing"));
		skipMissingIndicator = new Indicator(x + 111, y + 111, Components.immutableEmpty());
		Collections.addAll(placementSettingWidgets, skipMissingButton, skipMissingIndicator);

		skipBlockEntitiesButton = new IconButton(x + 129, y + 117, AllIcons.I_SKIP_BLOCK_ENTITIES);
		skipBlockEntitiesButton.withCallback(() -> {
			sendOptionUpdate(Option.SKIP_BLOCK_ENTITIES, !handler.contentHolder.replaceBlockEntities);
		});
		skipBlockEntitiesButton.setToolTip(Lang.translateDirect("gui.schematicannon.option.skipBlockEntities"));
		skipBlockEntitiesIndicator = new Indicator(x + 129, y + 111, Components.immutableEmpty());
		Collections.addAll(placementSettingWidgets, skipBlockEntitiesButton, skipBlockEntitiesIndicator);

		addRenderableWidgets(placementSettingWidgets);
	}

	protected boolean placementSettingsHidden() {
		return showSettingsIndicator.state == State.OFF;
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();

		SchematicannonBlockEntity be = handler.contentHolder;

		if (!placementSettingsHidden()) {
			for (int replaceMode = 0; replaceMode < replaceLevelButtons.size(); replaceMode++) {
				replaceLevelButtons.get(replaceMode).active = replaceMode != be.replaceMode;
				replaceLevelIndicators.get(replaceMode).state = replaceMode == be.replaceMode ? State.ON : State.OFF;
			}
			skipMissingIndicator.state = be.skipMissing ? State.ON : State.OFF;
			skipBlockEntitiesIndicator.state = !be.replaceBlockEntities ? State.ON : State.OFF;
		}

		playIndicator.state = State.OFF;
		pauseIndicator.state = State.OFF;
		resetIndicator.state = State.OFF;

		switch (be.state) {
		case PAUSED:
			pauseIndicator.state = State.YELLOW;
			playButton.active = true;
			pauseButton.active = false;
			resetButton.active = true;
			break;
		case RUNNING:
			playIndicator.state = State.GREEN;
			playButton.active = false;
			pauseButton.active = true;
			resetButton.active = true;
			break;
		case STOPPED:
			resetIndicator.state = State.RED;
			playButton.active = true;
			pauseButton.active = false;
			resetButton.active = false;
			break;
		default:
			break;
		}

		handleTooltips();
	}

	protected void handleTooltips() {
		if (placementSettingsHidden())
			return;

		for (ClickableWidget w : placementSettingWidgets)
			if (w instanceof IconButton) {
				IconButton button = (IconButton) w;
				if (!button.getToolTip()
					.isEmpty()) {
					button.setToolTip(button.getToolTip()
						.get(0));
					button.getToolTip()
						.add(TooltipHelper.holdShift(Palette.BLUE, hasShiftDown()));
				}
			}

		if (hasShiftDown()) {
			fillToolTip(skipMissingButton, skipMissingIndicator, "skipMissing");
			fillToolTip(skipBlockEntitiesButton, skipBlockEntitiesIndicator, "skipBlockEntities");
			fillToolTip(replaceLevelButtons.get(0), replaceLevelIndicators.get(0), "dontReplaceSolid");
			fillToolTip(replaceLevelButtons.get(1), replaceLevelIndicators.get(1), "replaceWithSolid");
			fillToolTip(replaceLevelButtons.get(2), replaceLevelIndicators.get(2), "replaceWithAny");
			fillToolTip(replaceLevelButtons.get(3), replaceLevelIndicators.get(3), "replaceWithEmpty");
		}
	}

	private void fillToolTip(IconButton button, Indicator indicator, String tooltipKey) {
		if (!button.isHovered())
			return;
		boolean enabled = indicator.state == State.ON;
		List<Text> tip = button.getToolTip();
		tip.add((enabled ? optionEnabled : optionDisabled).copyContentOnly()
			.formatted(BLUE));
		tip.addAll(TooltipHelper
			.cutTextComponent(Lang.translateDirect("gui.schematicannon.option." + tooltipKey + ".description"), Palette.ALL_GRAY));
	}

	@Override
	protected void drawBackground(DrawContext graphics, float partialTicks, int mouseX, int mouseY) {
		int invX = getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.width);
		int invY = y + BG_TOP.height + BG_BOTTOM.height + 2;
		renderPlayerInventory(graphics, invX, invY);

//		int x = x;
//		int y = y;

		BG_TOP.render(graphics, x, y);
		BG_BOTTOM.render(graphics, x, y + BG_TOP.height);

		SchematicannonBlockEntity be = handler.contentHolder;
		renderPrintingProgress(graphics, x, y, be.schematicProgress);
		renderFuelBar(graphics, x, y, be.fuelLevel);
		renderChecklistPrinterProgress(graphics, x, y, be.bookPrintingProgress);

		if (!be.inventory.getStackInSlot(0)
			.isEmpty())
			renderBlueprintHighlight(graphics, x, y);

		GuiGameElement.of(renderedItem).<GuiGameElement
			.GuiRenderBuilder>at(x + BG_TOP.width, y + BG_TOP.height + BG_BOTTOM.height - 48, -200)
			.scale(5)
			.render(graphics);

		graphics.drawCenteredTextWithShadow(textRenderer, title, x + (BG_TOP.width - 8) / 2, y + 3, 0xFFFFFF);

		Text msg = Lang.translateDirect("schematicannon.status." + be.statusMsg);
		int stringWidth = textRenderer.getWidth(msg);

		if (be.missingItem != null) {
			stringWidth += 16;
			GuiGameElement.of(be.missingItem).<GuiGameElement
				.GuiRenderBuilder>at(x + 128, y + 49, 100)
				.scale(1)
				.render(graphics);
		}

		graphics.drawTextWithShadow(textRenderer, msg, x + 103 - stringWidth / 2, y + 53, 0xCCDDFF);

		if ("schematicErrored".equals(be.statusMsg))
			graphics.drawTextWithShadow(textRenderer, Lang.translateDirect("schematicannon.status.schematicErroredCheckLogs"),
				x + 103 - stringWidth / 2, y + 65, 0xCCDDFF);
	}

	protected void renderBlueprintHighlight(DrawContext graphics, int x, int y) {
		AllGuiTextures.SCHEMATICANNON_HIGHLIGHT.render(graphics, x + 10, y + 60);
	}

	protected void renderPrintingProgress(DrawContext graphics, int x, int y, float progress) {
		progress = Math.min(progress, 1);
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_PROGRESS;
		graphics.drawTexture(sprite.location, x + 44, y + 64, sprite.startX, sprite.startY, (int) (sprite.width * progress), sprite.height);
	}

	protected void renderChecklistPrinterProgress(DrawContext graphics, int x, int y, float progress) {
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_CHECKLIST_PROGRESS;
		graphics.drawTexture(sprite.location, x + 154, y + 20, sprite.startX, sprite.startY, (int) (sprite.width * progress),
			sprite.height);
	}

	protected void renderFuelBar(DrawContext graphics, int x, int y, float amount) {
		AllGuiTextures sprite = AllGuiTextures.SCHEMATICANNON_FUEL;
		if (handler.contentHolder.hasCreativeCrate) {
			AllGuiTextures.SCHEMATICANNON_FUEL_CREATIVE.render(graphics, x + 36, y + 19);
			return;
		}
		graphics.drawTexture(sprite.location, x + 36, y + 19, sprite.startX, sprite.startY, (int) (sprite.width * amount), sprite.height);
	}

	@Override
	protected void renderForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		SchematicannonBlockEntity be = handler.contentHolder;

		int sx = x;
		int sy = y;

		int fuelX = sx + 36, fuelY = sy + 19;
		if (mouseX >= fuelX && mouseY >= fuelY && mouseX <= fuelX + AllGuiTextures.SCHEMATICANNON_FUEL.width
			&& mouseY <= fuelY + AllGuiTextures.SCHEMATICANNON_FUEL.height) {
			List<Text> tooltip = getFuelLevelTooltip(be);
			graphics.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
		}

		if (focusedSlot != null && !focusedSlot.hasStack()) {
			if (focusedSlot.id == 0)
				graphics.drawTooltip(textRenderer,
					TooltipHelper.cutTextComponent(Lang.translateDirect(_slotSchematic), Palette.GRAY_AND_BLUE), mouseX,
					mouseY);
			if (focusedSlot.id == 2)
				graphics.drawTooltip(textRenderer,
					TooltipHelper.cutTextComponent(Lang.translateDirect(_slotListPrinter), Palette.GRAY_AND_BLUE),
					mouseX, mouseY);
			if (focusedSlot.id == 4)
				graphics.drawTooltip(textRenderer,
					TooltipHelper.cutTextComponent(Lang.translateDirect(_slotGunpowder), Palette.GRAY_AND_BLUE), mouseX,
					mouseY);
		}

		if (be.missingItem != null) {
			int missingBlockX = sx + 128, missingBlockY = sy + 49;
			if (mouseX >= missingBlockX && mouseY >= missingBlockY && mouseX <= missingBlockX + 16
				&& mouseY <= missingBlockY + 16) {
				graphics.drawItemTooltip(textRenderer, be.missingItem, mouseX, mouseY);
			}
		}

		int paperX = sx + 112, paperY = sy + 19;
		if (mouseX >= paperX && mouseY >= paperY && mouseX <= paperX + 16 && mouseY <= paperY + 16)
			graphics.drawTooltip(textRenderer, listPrinter, mouseX, mouseY);

		super.renderForeground(graphics, mouseX, mouseY, partialTicks);
	}

	protected List<Text> getFuelLevelTooltip(SchematicannonBlockEntity be) {
		double fuelUsageRate = be.getFuelUsageRate();
		int shotsLeft = (int) (be.fuelLevel / fuelUsageRate);
		int shotsLeftWithItems = (int) (shotsLeft + be.inventory.getStackInSlot(4)
			.getCount() * (be.getFuelAddedByGunPowder() / fuelUsageRate));
		List<Text> tooltip = new ArrayList<>();

		if (be.hasCreativeCrate) {
			tooltip.add(Lang.translateDirect(_gunpowderLevel, "" + 100));
			tooltip.add(Components.literal("(").append(AllBlocks.CREATIVE_CRATE.get()
				.getName())
				.append(")")
				.formatted(DARK_PURPLE));
			return tooltip;
		}

		int fillPercent = (int) (be.fuelLevel * 100);
		tooltip.add(Lang.translateDirect(_gunpowderLevel, fillPercent));
		tooltip.add(Lang.translateDirect(_shotsRemaining, Components.literal(Integer.toString(shotsLeft)).formatted(BLUE))
			.formatted(GRAY));
		if (shotsLeftWithItems != shotsLeft)
			tooltip.add(Lang
				.translateDirect(_shotsRemainingWithBackup,
					Components.literal(Integer.toString(shotsLeftWithItems)).formatted(BLUE))
				.formatted(GRAY));

		return tooltip;
	}

	protected void sendOptionUpdate(Option option, boolean set) {
		AllPackets.getChannel().sendToServer(new ConfigureSchematicannonPacket(option, set));
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}

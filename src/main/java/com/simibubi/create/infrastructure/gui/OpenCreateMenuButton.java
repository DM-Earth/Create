package com.simibubi.create.infrastructure.gui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableTextContent;

public class OpenCreateMenuButton extends ButtonWidget {

	public static final ItemStack ICON = AllItems.GOGGLES.asStack();

	public OpenCreateMenuButton(int x, int y) {
		super(x, y, 20, 20, Components.immutableEmpty(), OpenCreateMenuButton::click, DEFAULT_NARRATION_SUPPLIER);
	}

	@Override
	public void drawMessage(DrawContext graphics, TextRenderer pFont, int pColor) {
		graphics.drawItem(ICON, getX() + 2, getY() + 2);
	}

	public static void click(ButtonWidget b) {
		ScreenOpener.open(new CreateMainMenuScreen(MinecraftClient.getInstance().currentScreen));
	}

	public static class SingleMenuRow {
		public final String left, right;

		public SingleMenuRow(String left, String right) {
			this.left = left;
			this.right = right;
		}

		public SingleMenuRow(String center) {
			this(center, center);
		}
	}

	public static class MenuRows {
		public static final MenuRows MAIN_MENU = new MenuRows(Arrays.asList(
				new SingleMenuRow("menu.singleplayer"),
				new SingleMenuRow("menu.multiplayer"),
				new SingleMenuRow("menu.online"),
				new SingleMenuRow("narrator.button.language", "narrator.button.accessibility")
		));

		public static final MenuRows INGAME_MENU = new MenuRows(Arrays.asList(
				new SingleMenuRow("menu.returnToGame"),
				new SingleMenuRow("gui.advancements", "gui.stats"),
				new SingleMenuRow("menu.sendFeedback", "menu.reportBugs"),
				new SingleMenuRow("menu.options", "menu.shareToLan"),
				new SingleMenuRow("menu.returnToMenu")
		));

		protected final List<String> leftButtons, rightButtons;

		public MenuRows(List<SingleMenuRow> variants) {
			leftButtons = variants.stream().map(r -> r.left).collect(Collectors.toList());
			rightButtons = variants.stream().map(r -> r.right).collect(Collectors.toList());
		}
	}

	public static class OpenConfigButtonHandler {

		public static void onGuiInit(MinecraftClient client, Screen gui, int scaledWidth, int scaledHeight) {
			MenuRows menu = null;
			int rowIdx = 0, offsetX = 0;
			if (gui instanceof TitleScreen) {
				menu = MenuRows.MAIN_MENU;
				rowIdx = AllConfigs.client().mainMenuConfigButtonRow.get();
				offsetX = AllConfigs.client().mainMenuConfigButtonOffsetX.get();
			} else if (gui instanceof GameMenuScreen) {
				menu = MenuRows.INGAME_MENU;
				rowIdx = AllConfigs.client().ingameMenuConfigButtonRow.get();
				offsetX = AllConfigs.client().ingameMenuConfigButtonOffsetX.get();
			}

			if (rowIdx != 0 && menu != null) {
				boolean onLeft = offsetX < 0;
				String target = (onLeft ? menu.leftButtons : menu.rightButtons).get(rowIdx - 1);

				int offsetX_ = offsetX;
				Screens.getButtons(gui).stream()
						.filter(w -> w.getMessage().getContent() instanceof TranslatableTextContent translatable
								&& translatable.getKey().equals(target))
						.findFirst()
						.ifPresent(w -> {
							gui.addDrawableChild(
									new OpenCreateMenuButton(w.getX() + offsetX_ + (onLeft ? -20 : w.getWidth()), w.getY())
							);
						});
			}
		}

	}

}

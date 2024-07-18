package com.simibubi.create.foundation.ponder.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.ponder.PonderChapter;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public class PonderIndexScreen extends NavigatableSimiScreen {

	protected final List<PonderChapter> chapters;
	private final double chapterXmult = 0.5;
	private final double chapterYmult = 0.3;
	protected Rect2i chapterArea;

	protected final List<Item> items;
	private final double itemXmult = 0.5;
	private double itemYmult = 0.75;
	protected Rect2i itemArea;

	private ItemStack hoveredItem = ItemStack.EMPTY;

	public PonderIndexScreen() {
		chapters = new ArrayList<>();
		items = new ArrayList<>();
	}

	@Override
	protected void init() {
		super.init();

		chapters.clear();
		// chapters.addAll(PonderRegistry.CHAPTERS.getAllChapters());

		items.clear();
		PonderRegistry.ALL.keySet()
			.stream()
			.map(key -> {
				return Registries.ITEM.getOrEmpty(key)
						.or(() -> Registries.BLOCK.getOrEmpty(key).map(Block::asItem))
						.orElse(null);
			})
			.filter(Objects::nonNull)
			.filter(PonderIndexScreen::exclusions)
			.forEach(items::add);

		boolean hasChapters = !chapters.isEmpty();

		// setup chapters
		LayoutHelper layout = LayoutHelper.centeredHorizontal(chapters.size(),
			MathHelper.clamp((int) Math.ceil(chapters.size() / 4f), 1, 4), 200, 38, 16);
		chapterArea = layout.getArea();
		int chapterCenterX = (int) (width * chapterXmult);
		int chapterCenterY = (int) (height * chapterYmult);

		// todo at some point pagination or horizontal scrolling may be needed for
		// chapters/items
		for (PonderChapter chapter : chapters) {
			ChapterLabel label = new ChapterLabel(chapter, chapterCenterX + layout.getX(),
				chapterCenterY + layout.getY(), (mouseX, mouseY) -> {
					centerScalingOn(mouseX, mouseY);
					ScreenOpener.transitionTo(PonderUI.of(chapter));
				});

			addDrawableChild(label);
			layout.next();
		}

		// setup items
		if (!hasChapters) {
			itemYmult = 0.5;
		}

		int maxItemRows = hasChapters ? 4 : 7;
		layout = LayoutHelper.centeredHorizontal(items.size(),
			MathHelper.clamp((int) Math.ceil(items.size() / 11f), 1, maxItemRows), 28, 28, 8);
		itemArea = layout.getArea();
		int itemCenterX = (int) (width * itemXmult);
		int itemCenterY = (int) (height * itemYmult);

		for (Item item : items) {
			PonderButton b = new PonderButton(itemCenterX + layout.getX() + 4, itemCenterY + layout.getY() + 4)
					.showing(new ItemStack(item))
					.withCallback((x, y) -> {
						if (!PonderRegistry.ALL.containsKey(RegisteredObjects.getKeyOrThrow(item)))
							return;

						centerScalingOn(x, y);
						ScreenOpener.transitionTo(PonderUI.of(new ItemStack(item)));
					});

			addDrawableChild(b);
			layout.next();
		}

	}

	@Override
	protected void initBackTrackIcon(PonderButton backTrack) {
		backTrack.showing(AllItems.WRENCH.asStack());
	}

	private static boolean exclusions(Item item) {
		if (item instanceof BlockItem) {
			Block block = ((BlockItem) item).getBlock();
			if (block instanceof ValveHandleBlock && !AllBlocks.COPPER_VALVE_HANDLE.is(item))
				return false;
		}

		return true;
	}

	@Override
	public void tick() {
		super.tick();
		PonderUI.ponderTicks++;

		hoveredItem = ItemStack.EMPTY;
		Window w = client.getWindow();
		double mouseX = client.mouse.getX() * w.getScaledWidth() / w.getWidth();
		double mouseY = client.mouse.getY() * w.getScaledHeight() / w.getHeight();
		for (Element child : children()) {
			if (child instanceof PonderButton button) {
				if (button.isMouseOver(mouseX, mouseY)) {
					hoveredItem = button.getItem();
				}
			}
		}
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		int x = (int) (width * chapterXmult);
		int y = (int) (height * chapterYmult);

		MatrixStack ms = graphics.getMatrices();

		if (!chapters.isEmpty()) {
			ms.push();
			ms.translate(x, y, 0);

			UIRenderHelper.streak(graphics, 0, chapterArea.getX() - 10, chapterArea.getY() - 20, 20, 220);
			graphics.drawText(textRenderer, "Topics to Ponder about", chapterArea.getX() - 5, chapterArea.getY() - 25, Theme.i(Theme.Key.TEXT), false);

			ms.pop();
		}

		x = (int) (width * itemXmult);
		y = (int) (height * itemYmult);

		ms.push();
		ms.translate(x, y, 0);

		UIRenderHelper.streak(graphics, 0, itemArea.getX() - 10, itemArea.getY() - 20, 20, 220);
		graphics.drawText(textRenderer, "Items to inspect", itemArea.getX() - 5, itemArea.getY() - 25, Theme.i(Theme.Key.TEXT), false);

		ms.pop();
	}

	@Override
	protected void renderWindowForeground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (hoveredItem.isEmpty())
			return;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, 200);

		graphics.drawItemTooltip(textRenderer, hoveredItem, mouseX, mouseY);

		ms.pop();
	}

	/*@Override
	public boolean mouseClicked(double x, double y, int button) {
		MutableBoolean handled = new MutableBoolean(false);
		widgets.forEach(w -> {
			if (handled.booleanValue())
				return;
			if (!w.isMouseOver(x, y))
				return;
			if (w instanceof PonderButton) {
				PonderButton btn = (PonderButton) w;
				btn.runCallback(x, y);
				handled.setTrue();
			}
		});

		if (handled.booleanValue())
			return true;
		return super.mouseClicked(x, y, button);
	}*/

	@Override
	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		return other instanceof PonderIndexScreen;
	}

	public ItemStack getHoveredTooltipItem() {
		return hoveredItem;
	}

	@Override
	public boolean shouldPause() {
		return true;
	}
}

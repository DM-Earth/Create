package com.simibubi.create.foundation.config.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Window;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.config.ui.entries.NumberEntry;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.TickableGuiEventListener;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.element.TextStencilElement;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

public class ConfigScreenList extends AlwaysSelectedEntryListWidget<ConfigScreenList.Entry> implements TickableGuiEventListener {

	public static TextFieldWidget currentText;

	public ConfigScreenList(MinecraftClient client, int width, int height, int top, int bottom, int elementHeight) {
		super(client, width, height, top, bottom, elementHeight);
		setRenderBackground(false);
		setRenderHorizontalShadows(false);
		setRenderSelection(false);
		currentText = null;
		headerHeight = 3;
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		Color c = new Color(0x60_000000);
		UIRenderHelper.angledGradient(graphics, 90, left + width / 2, top, width, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, -90, left + width / 2, bottom, width, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, 0, left, top + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
		UIRenderHelper.angledGradient(graphics, 180, right, top + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);

		super.render(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderList(DrawContext graphics, int p_239229_, int p_239230_, float p_239231_) {
		Window window = client.getWindow();
		double d0 = window.getScaleFactor();
		RenderSystem.enableScissor((int) (this.left * d0), (int) (window.getFramebufferHeight() - (this.bottom * d0)), (int) (this.width * d0), (int) (this.height * d0));
		super.renderList(graphics, p_239229_, p_239230_, p_239231_);
		RenderSystem.disableScissor();
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		children().stream().filter(e -> e instanceof NumberEntry<?>).forEach(e -> e.mouseClicked(x, y, button));

		return super.mouseClicked(x, y, button);
	}

	@Override
	public int getRowWidth() {
		return width - 16;
	}

	@Override
	protected int getScrollbarPositionX() {
		return left + this.width - 6;
	}

	@Override
	public void tick() {
		/*for(int i = 0; i < getItemCount(); ++i) {
			int top = this.getRowTop(i);
			int bot = top + itemHeight;
			if (bot >= this.y0 && top <= this.y1)
				this.getEntry(i).tick();
		}*/
		children().forEach(com.simibubi.create.foundation.config.ui.ConfigScreenList.Entry::tick);

	}

	public boolean search(String query) {
		if (query == null || query.isEmpty()) {
			setScrollAmount(0);
			return true;
		}

		String q = query.toLowerCase(Locale.ROOT);
		Optional<com.simibubi.create.foundation.config.ui.ConfigScreenList.Entry> first = children().stream().filter(entry -> {
			if (entry.path == null)
				return false;

			String[] split = entry.path.split("\\.");
			String key = split[split.length - 1].toLowerCase(Locale.ROOT);
			return key.contains(q);
		}).findFirst();

		if (!first.isPresent()) {
			setScrollAmount(0);
			return false;
		}

		com.simibubi.create.foundation.config.ui.ConfigScreenList.Entry e = first.get();
		e.annotations.put("highlight", "(:");
		centerScrollOn(e);
		return true;
	}

	public void bumpCog(float force) {
		ConfigScreen.cogSpin.bump(3, force);
	}

	public static abstract class Entry extends AlwaysSelectedEntryListWidget.Entry<com.simibubi.create.foundation.config.ui.ConfigScreenList.Entry> implements TickableGuiEventListener {
		protected List<Element> listeners;
		public Map<String, String> annotations;
		public String path;

		protected Entry() {
			listeners = new ArrayList<>();
			annotations = new HashMap<>();
		}

		@Override
		public boolean mouseClicked(double x, double y, int button) {
			return getGuiListeners().stream().anyMatch(l -> l.mouseClicked(x, y, button));
		}

		@Override
		public boolean keyPressed(int code, int keyPressed_2_, int keyPressed_3_) {
			return getGuiListeners().stream().anyMatch(l -> l.keyPressed(code, keyPressed_2_, keyPressed_3_));
		}

		@Override
		public boolean charTyped(char ch, int code) {
			return getGuiListeners().stream().anyMatch(l -> l.charTyped(ch, code));
		}

		@Override
		public void tick() {}

		public List<Element> getGuiListeners() {
			return listeners;
		}

		public void setEditable(boolean b) {}

		protected boolean isCurrentValueChanged() {
			if (path == null) {
				return false;
			}
			return ConfigHelper.changes.containsKey(path);
		}
	}

	public static class LabeledEntry extends com.simibubi.create.foundation.config.ui.ConfigScreenList.Entry {

		protected static final float labelWidthMult = 0.4f;

		public TextStencilElement label;
		protected List<Text> labelTooltip;
		protected String unit = null;
		protected LerpedFloat differenceAnimation = LerpedFloat.linear().startWithValue(0);
		protected LerpedFloat highlightAnimation = LerpedFloat.linear().startWithValue(0);

		public LabeledEntry(String label) {
			this.label = new TextStencilElement(MinecraftClient.getInstance().textRenderer, label);
			this.label.withElementRenderer((graphics, width, height, alpha) -> UIRenderHelper.angledGradient(graphics,
				0, 0, height / 2, height, width, Theme.p(Theme.Key.TEXT_ACCENT_STRONG)));
			labelTooltip = new ArrayList<>();
		}

		public LabeledEntry(String label, String path) {
			this(label);
			this.path = path;
		}

		@Override
		public void tick() {
			differenceAnimation.tickChaser();
			highlightAnimation.tickChaser();
			super.tick();
		}

		@Override
		public void render(DrawContext graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
			if (isCurrentValueChanged()) {
				if (differenceAnimation.getChaseTarget() != 1)
					differenceAnimation.chase(1, .5f, LerpedFloat.Chaser.EXP);
			} else {
				if (differenceAnimation.getChaseTarget() != 0)
					differenceAnimation.chase(0, .6f, LerpedFloat.Chaser.EXP);
			}

			float animation = differenceAnimation.getValue(partialTicks);
			if (animation > .1f) {
				int offset = (int) (30 * (1 - animation));

				if (annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName())) {
					UIRenderHelper.streak(graphics, 180, x + width + 10 + offset, y + height / 2, height - 6, 110, new Color(0x50_601010));
				} else if (annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName())) {
					UIRenderHelper.streak(graphics, 180, x + width + 10 + offset, y + height / 2, height - 6, 110, new Color(0x40_eefb17));
				}

				UIRenderHelper.breadcrumbArrow(graphics, x - 10 - offset, y + 6, 0, -20, 24, -18, new Color(0x70_ffffff), Color.TRANSPARENT_BLACK);
			}

			UIRenderHelper.streak(graphics, 0, x - 10, y + height / 2, height - 6, width / 8 * 7, 0xdd_000000);
			UIRenderHelper.streak(graphics, 180, x + (int) (width * 1.35f) + 10, y + height / 2, height - 6, width / 8 * 7, 0xdd_000000);
			MutableText component = label.getComponent();
			TextRenderer font = MinecraftClient.getInstance().textRenderer;
			if (font.getWidth(component) > getLabelWidth(width) - 10) {
				label.withText(font.trimToWidth(component, getLabelWidth(width) - 15).getString() + "...");
			}
			if (unit != null) {
				int unitWidth = font.getWidth(unit);
				graphics.drawText(font, unit, x + getLabelWidth(width) - unitWidth - 5, y + height / 2 + 2, Theme.i(Theme.Key.TEXT_DARKER), false);
				label.at(x + 10, y + height / 2 - 10, 0).render(graphics);
			} else {
				label.at(x + 10, y + height / 2 - 4, 0).render(graphics);
			}

			if (annotations.containsKey("highlight")) {
				highlightAnimation.startWithValue(1).chase(0, 0.1f, LerpedFloat.Chaser.LINEAR);
				annotations.remove("highlight");
			}

			animation = highlightAnimation.getValue(partialTicks);
			if (animation > .01f) {
				Color highlight = new Color(0xa0_ffffff).scaleAlpha(animation);
				UIRenderHelper.streak(graphics, 0, x - 10, y + height / 2, height - 6, 5, highlight);
				UIRenderHelper.streak(graphics, 180, x + width, y + height / 2, height - 6, 5, highlight);
				UIRenderHelper.streak(graphics, 90, x + width / 2 - 5, y + 3, width + 10, 5, highlight);
				UIRenderHelper.streak(graphics, -90, x + width / 2 - 5, y + height - 3, width + 10, 5, highlight);
			}


			if (mouseX > x && mouseX < x + getLabelWidth(width) && mouseY > y + 5 && mouseY < y + height - 5) {
				List<Text> tooltip = getLabelTooltip();
				if (tooltip.isEmpty())
					return;

				RenderSystem.disableScissor();
				Screen screen = MinecraftClient.getInstance().currentScreen;
				RemovedGuiUtils.drawHoveringText(graphics, tooltip, mouseX, mouseY, screen.width, screen.height, 700, font);
				GlStateManager._enableScissorTest();
			}
		}

		public List<Text> getLabelTooltip() {
			return labelTooltip;
		}

		protected int getLabelWidth(int totalWidth) {
			return totalWidth;
		}

		// TODO 1.17
		@Override
		public Text getNarration() {
			return Components.immutableEmpty();
		}
	}
}

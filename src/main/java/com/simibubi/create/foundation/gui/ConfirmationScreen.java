package com.simibubi.create.foundation.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.element.BoxElement;
import com.simibubi.create.foundation.gui.element.TextStencilElement;
import com.simibubi.create.foundation.gui.widget.BoxWidget;

public class ConfirmationScreen extends AbstractSimiScreen {

	private Screen source;
	private Consumer<Response> action = _success -> {
	};
	private List<StringVisitable> text = new ArrayList<>();
	private boolean centered = false;
	private int x;
	private int y;
	private int textWidth;
	private int textHeight;
	private boolean tristate;

	private BoxWidget confirm;
	private BoxWidget confirmDontSave;
	private BoxWidget cancel;
	private BoxElement textBackground;

	public enum Response {
		Confirm, ConfirmDontSave, Cancel
	}

	/*
	 * Removes text lines from the back of the list
	 * */
	public ConfirmationScreen removeTextLines(int amount) {
		if (amount > text.size())
			return clearText();

		text.subList(text.size() - amount, text.size()).clear();
		return this;
	}

	public ConfirmationScreen clearText() {
		this.text.clear();
		return this;
	}

	public ConfirmationScreen addText(StringVisitable text) {
		this.text.add(text);
		return this;
	}

	public ConfirmationScreen withText(StringVisitable text) {
		return clearText().addText(text);
	}

	public ConfirmationScreen at(int x, int y) {
		this.x = Math.max(x, 0);
		this.y = Math.max(y, 0);
		this.centered = false;
		return this;
	}

	public ConfirmationScreen centered() {
		this.centered = true;
		return this;
	}

	public ConfirmationScreen withAction(Consumer<Boolean> action) {
		this.action = r -> action.accept(r == Response.Confirm);
		return this;
	}

	public ConfirmationScreen withThreeActions(Consumer<Response> action) {
		this.action = action;
		this.tristate = true;
		return this;
	}

	public void open(@Nonnull Screen source) {
		this.source = source;
		MinecraftClient client = MinecraftClient.getInstance();
		this.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
		this.client.currentScreen = this;
	}

	@Override
	public void tick() {
		super.tick();
		source.tick();
	}

	@Override
	protected void init() {
		super.init();

		ArrayList<StringVisitable> copy = new ArrayList<>(text);
		text.clear();
		copy.forEach(t -> text.addAll(textRenderer.getTextHandler().wrapLines(t, 300, Style.EMPTY)));

		textHeight = text.size() * (textRenderer.fontHeight + 1) + 4;
		textWidth = 300;

		if (centered) {
			x = width/2 - textWidth/2 - 2;
			y = height/2 - textHeight/2 - 16;
		} else {
			x = Math.max(0, x - textWidth / 2);
			y = Math.max(0, y -= textHeight);
		}

		if (x + textWidth > width) {
			x = width - textWidth;
		}

		if (y + textHeight + 30 > height) {
			y = height - textHeight - 30;
		}

		int buttonX = x + textWidth / 2 - 6 - (int) (70 * (tristate ? 1.5f : 1));

		TextStencilElement confirmText =
				new TextStencilElement(textRenderer, tristate ? "Save" : "Confirm").centered(true, true);
		confirm = new BoxWidget(buttonX, y + textHeight + 6, 70, 16).withCallback(() -> accept(Response.Confirm));
		confirm.showingElement(confirmText.withElementRenderer(BoxWidget.gradientFactory.apply(confirm)));
		addDrawableChild(confirm);

		buttonX += 12 + 70;

		if (tristate) {
			TextStencilElement confirmDontSaveText =
					new TextStencilElement(textRenderer, "Don't Save").centered(true, true);
			confirmDontSave =
					new BoxWidget(buttonX, y + textHeight + 6, 70, 16).withCallback(() -> accept(Response.ConfirmDontSave));
			confirmDontSave.showingElement(
					confirmDontSaveText.withElementRenderer(BoxWidget.gradientFactory.apply(confirmDontSave)));
			addDrawableChild(confirmDontSave);
			buttonX += 12 + 70;
		}

		TextStencilElement cancelText = new TextStencilElement(textRenderer, "Cancel").centered(true, true);
		cancel = new BoxWidget(buttonX, y + textHeight + 6, 70, 16)
				.withCallback(() -> accept(Response.Cancel));
		cancel.showingElement(cancelText.withElementRenderer(BoxWidget.gradientFactory.apply(cancel)));
		addDrawableChild(cancel);

		textBackground = new BoxElement()
				.gradientBorder(Theme.p(Theme.Key.BUTTON_DISABLE))
				.withBounds(width + 10, textHeight + 35)
				.at(-5, y - 5);

		if (text.size() == 1)
			x = (width - textRenderer.getWidth(text.get(0))) / 2;
	}

	@Override
	public void close() {
		accept(Response.Cancel);
	}

	private void accept(Response success) {
		client.currentScreen = source;
		action.accept(success);
	}

	@Override
	protected void renderWindow(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		textBackground.render(graphics);
		int offset = textRenderer.fontHeight + 1;
		int lineY = y - offset;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(0, 0, 200);

		for (StringVisitable line : text) {
			lineY += offset;
			if (line == null)
				continue;
			graphics.drawText(textRenderer, line.getString(), x, lineY, 0xeaeaea, false);
		}

		ms.pop();
	}

	@Override
	protected void renderWindowBackground(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		endFrame();

		source.render(graphics, 0, 0, 10); // zero mouse coords to prevent further tooltips

		prepareFrame();

		graphics.fillGradient(0, 0, this.width, this.height, 0x70101010, 0x80101010);
	}


	@Override
	protected void prepareFrame() {
		UIRenderHelper.swapAndBlitColor(client.getFramebuffer(), UIRenderHelper.framebuffer);
		RenderSystem.clear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
	}

	@Override
	protected void endFrame() {
		UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, client.getFramebuffer());
	}

	@Override
	public void resize(@Nonnull MinecraftClient client, int width, int height) {
		super.resize(client, width, height);
		source.resize(client, width, height);
	}

	@Override
	public boolean shouldPause() {
		return true;
	}
}

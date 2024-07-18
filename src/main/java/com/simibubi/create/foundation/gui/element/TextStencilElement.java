package com.simibubi.create.foundation.gui.element;

import com.simibubi.create.foundation.utility.Components;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;

public class TextStencilElement extends DelegatedStencilElement {

	protected TextRenderer font;
	protected MutableText component;
	protected boolean centerVertically = false;
	protected boolean centerHorizontally = false;

	public TextStencilElement(TextRenderer font) {
		super();
		this.font = font;
		height = 10;
	}

	public TextStencilElement(TextRenderer font, String text) {
		this(font);
		component = Components.literal(text);
	}

	public TextStencilElement(TextRenderer font, MutableText component) {
		this(font);
		this.component = component;
	}

	public TextStencilElement withText(String text) {
		component = Components.literal(text);
		return this;
	}

	public TextStencilElement withText(MutableText component) {
		this.component = component;
		return this;
	}

	public TextStencilElement centered(boolean vertical, boolean horizontal) {
		this.centerVertically = vertical;
		this.centerHorizontally = horizontal;
		return this;
	}

	@Override
	protected void renderStencil(DrawContext graphics) {
		float x = 0, y = 0;
		if (centerHorizontally)
			x = width / 2f - font.getWidth(component) / 2f;

		if (centerVertically)
			y = height / 2f - (font.fontHeight - 1) / 2f;

		graphics.drawText(font, component, Math.round(x), Math.round(y), 0xff_000000, false);
	}

	@Override
	protected void renderElement(DrawContext graphics) {
		float x = 0, y = 0;
		if (centerHorizontally)
			x = width / 2f - font.getWidth(component) / 2f;

		if (centerVertically)
			y = height / 2f - (font.fontHeight - 1) / 2f;

		MatrixStack ms = graphics.getMatrices();
		ms.push();
		ms.translate(x, y, 0);
		element.render(graphics, font.getWidth(component), font.fontHeight + 2, alpha);
		ms.pop();
	}

	public MutableText getComponent() {
		return component;
	}
}

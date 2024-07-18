package com.simibubi.create.foundation.gui.element;

import javax.annotation.Nonnull;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class CombinedStencilElement extends StencilElement {

	private StencilElement element1;
	private StencilElement element2;
	private ElementMode mode;

	private CombinedStencilElement() {}

	public static CombinedStencilElement of(@Nonnull StencilElement element1, @Nonnull StencilElement element2) {
		return of(element1, element2, ElementMode.FIRST);
	}

	public static CombinedStencilElement of(@Nonnull StencilElement element1, @Nonnull StencilElement element2, ElementMode mode) {
		CombinedStencilElement e = new CombinedStencilElement();
		e.element1 = element1;
		e.element2 = element2;
		e.mode = mode;
		return e;
	}

	public <T extends CombinedStencilElement> T withFirst(StencilElement element) {
		this.element1 = element;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends CombinedStencilElement> T withSecond(StencilElement element) {
		this.element2 = element;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends CombinedStencilElement> T withMode(ElementMode mode) {
		this.mode = mode;
		//noinspection unchecked
		return (T) this;
	}

	@Override
	protected void renderStencil(DrawContext graphics) {
		MatrixStack ms = graphics.getMatrices();
		ms.push();
		element1.transform(ms);
		element1.withBounds(width, height);
		element1.renderStencil(graphics);
		ms.pop();
		ms.push();
		element2.transform(ms);
		element2.withBounds(width, height);
		element2.renderStencil(graphics);
		ms.pop();
	}

	@Override
	protected void renderElement(DrawContext graphics) {
		if (mode.rendersFirst())
			element1.<StencilElement>withBounds(width, height).renderElement(graphics);

		if (mode.rendersSecond())
			element2.<StencilElement>withBounds(width, height).renderElement(graphics);
	}

	public enum ElementMode {
		FIRST, SECOND, BOTH;

		boolean rendersFirst() {
			return this == FIRST || this == BOTH;
		}

		boolean rendersSecond() {
			return this == SECOND || this == BOTH;
		}
	}
}

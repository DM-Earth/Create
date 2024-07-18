package com.simibubi.create.compat.emi;

import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public record CyclingDrawable(List<? extends DrawableWidgetConsumer> children) implements DrawableWidgetConsumer {
	@Override
	public void render(DrawContext guiGraphics, int x, int y, float delta) {
		this.choose().render(guiGraphics, x, y, delta);
	}

	private DrawableWidgetConsumer choose() {
		// ticks are not available, use system time.
		long millis = System.currentTimeMillis();
		long seconds = millis / 1000;
		int index = (int) (seconds % this.children.size());
		return this.children.get(index);
	}
}

package com.simibubi.create.compat.rei.widgets;

import java.util.List;

import com.google.common.collect.Lists;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;

public class AnimatedKineticsWidget extends Widget {

	private Point pos;

	public void setPos(Point point) {
		this.pos = point;
	}

	public Point getPos() {
		return pos;
	}

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
		draw(graphics, pos.getX(), pos.getY());
	}

	@Override
	public List<? extends Element> children() {
		return Lists.newArrayList();
	}

	public void draw(DrawContext graphics, int xOffset, int yOffset) {
	}
}

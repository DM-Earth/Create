package com.simibubi.create.foundation.gui.widget;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class TooltipArea extends AbstractSimiWidget {

	public TooltipArea(int x, int y, int width, int height) {
		super(x, y, width, height);
	}

	@Override
	public void renderButton(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
		if (visible)
			hovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
	}

	public TooltipArea withTooltip(List<Text> tooltip) {
		this.toolTip = tooltip;
		return this;
	}

}
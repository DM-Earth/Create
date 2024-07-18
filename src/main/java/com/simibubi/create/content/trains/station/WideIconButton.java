package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.ScreenElement;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.gui.DrawContext;

public class WideIconButton extends IconButton {

	public WideIconButton(int x, int y, ScreenElement icon) {
		super(x, y, 26, 18, icon);
	}

	@Override
	protected void drawBg(DrawContext graphics, AllGuiTextures button) {
		super.drawBg(graphics, button);
		graphics.drawTexture(button.location, getX() + 9, getY(), button.startX + 1, button.startY, button.width - 1, button.height);
	}

}

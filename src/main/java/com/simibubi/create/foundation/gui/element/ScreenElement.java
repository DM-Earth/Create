package com.simibubi.create.foundation.gui.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;

public interface ScreenElement {

	@Environment(EnvType.CLIENT)
	void render(DrawContext graphics, int x, int y);

}

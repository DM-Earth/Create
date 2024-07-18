package com.simibubi.create.content.schematics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

public class SchematicHotbarSlotOverlay  {
	
	public void renderOn(DrawContext graphics, int slot) {
		Window mainWindow = MinecraftClient.getInstance().getWindow();
		int x = mainWindow.getScaledWidth() / 2 - 88;
		int y = mainWindow.getScaledHeight() - 19;
		RenderSystem.enableDepthTest();
		AllGuiTextures.SCHEMATIC_SLOT.render(graphics, x + 20 * slot, y);
	}

}

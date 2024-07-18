package com.simibubi.create.content.schematics.client.tools;

import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public interface ISchematicTool {

	public void init();
	public void updateSelection();

	public boolean handleRightClick();
	public boolean handleMouseWheel(double delta);
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera);
	public void renderOverlay(DrawContext graphics, float partialTicks, int width, int height);
	public void renderOnSchematic(MatrixStack ms, SuperRenderTypeBuffer buffer);

}

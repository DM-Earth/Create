package com.simibubi.create.content.schematics.client.tools;

import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public abstract class PlacementToolBase extends SchematicToolBase {

	@Override
	public void init() {
		super.init();
	}

	@Override
	public void updateSelection() {
		super.updateSelection();
	}

	@Override
	public void renderTool(MatrixStack ms, SuperRenderTypeBuffer buffer, Vec3d camera) {
		super.renderTool(ms, buffer, camera);
	}

	@Override
	public void renderOverlay(DrawContext graphics, float partialTicks, int width, int height) {
		super.renderOverlay(graphics, partialTicks, width, height);
	}

	@Override
	public boolean handleMouseWheel(double delta) {
		return false;
	}

	@Override
	public boolean handleRightClick() {
		return false;
	}

}

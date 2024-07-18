package com.simibubi.create.content.schematics.client.tools;

import com.simibubi.create.foundation.outliner.LineOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class RotateTool extends PlacementToolBase {

	private LineOutline line = new LineOutline();

	@Override
	public boolean handleMouseWheel(double delta) {
		schematicHandler.getTransformation()
			.rotate90(delta > 0);
		schematicHandler.markDirty();
		return true;
	}

	@Override
	public void renderOnSchematic(MatrixStack ms, SuperRenderTypeBuffer buffer) {
		Box bounds = schematicHandler.getBounds();
		double height = bounds.getYLength() + Math.max(20, bounds.getYLength());
		Vec3d center = bounds.getCenter()
			.add(schematicHandler.getTransformation()
				.getRotationOffset(false));
		Vec3d start = center.subtract(0, height / 2, 0);
		Vec3d end = center.add(0, height / 2, 0);

		line.getParams()
			.disableCull()
			.disableLineNormals()
			.colored(0xdddddd)
			.lineWidth(1 / 16f);
		line.set(start, end)
			.render(ms, buffer, Vec3d.ZERO, AnimationTickHolder.getPartialTicks());

		super.renderOnSchematic(ms, buffer);
	}

}

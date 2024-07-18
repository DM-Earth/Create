package com.simibubi.create.compat.rei.category.animations;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction.Axis;
import com.google.common.collect.Lists;
import com.simibubi.create.AllBlocks;

public class AnimatedCrushingWheels extends AnimatedKinetics {

	private final BlockState wheel = AllBlocks.CRUSHING_WHEEL.getDefaultState()
			.with(Properties.AXIS, Axis.X);

	@Override
	public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(getPos().getX(), getPos().getY(), 100);
		matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-22.5f));
		int scale = 22;

		blockElement(wheel)
				.rotateBlock(0, 90, -getCurrentAngle())
				.scale(scale)
				.render(graphics);

		blockElement(wheel)
				.rotateBlock(0, 90, getCurrentAngle())
				.atLocal(2, 0, 0)
				.scale(scale)
				.render(graphics);

		matrixStack.pop();
	}

	@Override
	public List<? extends Element> children() {
		return Lists.newArrayList();
	}

}

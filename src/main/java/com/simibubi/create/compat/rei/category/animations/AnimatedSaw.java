package com.simibubi.create.compat.rei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class AnimatedSaw extends AnimatedKinetics {

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 0);
		matrixStack.translate(0, 0, 200);
		matrixStack.translate(2, 22, 0);
		matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f + 90));
		int scale = 25;

		blockElement(shaft(Axis.X))
			.rotateBlock(-getCurrentAngle(), 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MECHANICAL_SAW.getDefaultState()
			.with(SawBlock.FACING, Direction.UP))
			.rotateBlock(0, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE)
			.rotateBlock(0, -90, -90)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();
	}

}

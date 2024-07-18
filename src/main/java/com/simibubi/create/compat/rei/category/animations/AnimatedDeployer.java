package com.simibubi.create.compat.rei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class AnimatedDeployer extends AnimatedKinetics {

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 100);
		matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(shaft(Axis.Z))
			.rotateBlock(0, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.DEPLOYER.getDefaultState()
			.with(DeployerBlock.FACING, Direction.DOWN)
			.with(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false))
			.scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float offset = cycle < 10 ? cycle / 10f : cycle < 20 ? (20 - cycle) / 10f : 0;

		matrixStack.push();

		matrixStack.translate(0, offset * 17, 0);
		blockElement(AllPartialModels.DEPLOYER_POLE)
			.rotateBlock(90, 0, 0)
			.scale(scale)
			.render(graphics);
		blockElement(AllPartialModels.DEPLOYER_HAND_HOLDING)
			.rotateBlock(90, 0, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();
	}

}

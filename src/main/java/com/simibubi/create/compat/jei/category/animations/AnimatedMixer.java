package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class AnimatedMixer extends AnimatedKinetics {

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 200);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 23;

		blockElement(cogwheel())
			.rotateBlock(0, getCurrentAngle() * 2, 0)
			.atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MECHANICAL_MIXER.getDefaultState())
			.atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		float animation = ((MathHelper.sin(AnimationTickHolder.getRenderTime() / 32f) + 1) / 5) + .5f;

		blockElement(AllPartialModels.MECHANICAL_MIXER_POLE)
			.atLocal(0, animation, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllPartialModels.MECHANICAL_MIXER_HEAD)
			.rotateBlock(0, getCurrentAngle() * 4, 0)
			.atLocal(0, animation, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.BASIN.getDefaultState())
			.atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();
	}

}

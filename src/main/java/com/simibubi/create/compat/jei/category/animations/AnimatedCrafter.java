package com.simibubi.create.compat.jei.category.animations;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class AnimatedCrafter extends AnimatedKinetics {

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 0);
		AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);

		matrixStack.translate(3, 16, 0);
		TransformStack.cast(matrixStack)
			.rotateX(-12.5f)
			.rotateY(-22.5f);
		int scale = 22;

		blockElement(cogwheel())
			.rotateBlock(90, 0, getCurrentAngle())
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MECHANICAL_CRAFTER.getDefaultState())
			.rotateBlock(0, 180, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();
	}

}

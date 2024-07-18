package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class AnimatedMillstone extends AnimatedKinetics {

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 0);
		AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);
		matrixStack.translate(-2, 18, 0);
		int scale = 22;

		blockElement(AllPartialModels.MILLSTONE_COG)
			.rotateBlock(22.5, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(AllBlocks.MILLSTONE.getDefaultState())
			.rotateBlock(22.5, 22.5, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.pop();
	}

}

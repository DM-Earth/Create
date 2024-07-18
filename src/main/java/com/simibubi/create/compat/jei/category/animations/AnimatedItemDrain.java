package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.UIRenderHelper;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class AnimatedItemDrain extends AnimatedKinetics {

	private FluidStack fluid;

	public AnimatedItemDrain withFluid(FluidStack fluid) {
		this.fluid = fluid;
		return this;
	}

	@Override
	public void draw(DrawContext graphics, int xOffset, int yOffset) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(xOffset, yOffset, 100);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.5f));
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(AllBlocks.ITEM_DRAIN.getDefaultState())
			.scale(scale)
			.render(graphics);

		Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance()
			.getBuffer());
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(scale, scale, scale);
		float from = 2/16f;
		float to = 1f - from;
		FluidRenderer.renderFluidBox(fluid, from, from, from, to, 3/4f, to, buffer, matrixStack, LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
		buffer.draw();

		matrixStack.pop();
	}
}

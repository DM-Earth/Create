package com.simibubi.create.compat.jei.category.animations;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

public class AnimatedSpout extends AnimatedKinetics {

	private List<FluidStack> fluids;

	public AnimatedSpout withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
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

		blockElement(AllBlocks.SPOUT.getDefaultState())
			.scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? MathHelper.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		matrixStack.push();

		blockElement(AllPartialModels.SPOUT_TOP)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_MIDDLE)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_BOTTOM)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);

		matrixStack.pop();

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
		Immediate buffer = VertexConsumerProvider.immediate(Tessellator.getInstance()
			.getBuffer());
		matrixStack.push();
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(16, 16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		FluidRenderer.renderFluidBox(fluids.get(0), from, from, from, to, to, to, buffer, matrixStack, LightmapTextureManager.MAX_LIGHT_COORDINATE, false);
		matrixStack.pop();

		float width = 1 / 128f * squeeze;
		matrixStack.translate(scale / 2f, scale * 1.5f, scale / 2f);
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(16, 16, 16);
		matrixStack.translate(-0.5f, 0, -0.5f);
		from = -width / 2 + 0.5f;
		to = width / 2 + 0.5f;
		FluidRenderer.renderFluidBox(fluids.get(0), from, 0, from, to, 2, to, buffer, matrixStack, LightmapTextureManager.MAX_LIGHT_COORDINATE,
			false);
		buffer.draw();
		DiffuseLighting.enableGuiDepthLighting();

		matrixStack.pop();
	}

}

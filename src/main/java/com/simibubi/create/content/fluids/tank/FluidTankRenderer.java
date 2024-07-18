package com.simibubi.create.content.fluids.tank;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;

import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class FluidTankRenderer extends SafeBlockEntityRenderer<FluidTankBlockEntity> {

	public FluidTankRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(FluidTankBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (!be.isController())
			return;
		if (!be.window) {
			if (be.boiler.isActive())
				renderAsBoiler(be, partialTicks, ms, buffer, light, overlay);
			return;
		}

		LerpedFloat fluidLevel = be.getFluidLevel();
		if (fluidLevel == null)
			return;

		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = be.height - 2 * capHeight - minPuddleHeight;

		float level = fluidLevel.getValue(partialTicks);
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = MathHelper.clamp(level * totalHeight, 0, totalHeight);

		FluidTank tank = be.tankInventory;
		FluidStack fluidStack = tank.getFluid();

		if (fluidStack.isEmpty())
			return;

		boolean top = FluidVariantAttributes.isLighterThanAir(fluidStack.getType());

		float xMin = tankHullWidth;
		float xMax = xMin + be.width - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + be.width - 2 * tankHullWidth;

		ms.push();
		ms.translate(0, clampedLevel - totalHeight, 0);
		FluidRenderer.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms, light, false);
		ms.pop();
	}

	protected void renderAsBoiler(FluidTankBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		ms.push();
		TransformStack msr = TransformStack.cast(ms);
		msr.translate(be.width / 2f, 0.5, be.width / 2f);

		float dialPivot = 5.75f / 16;
		float progress = be.boiler.gauge.getValue(partialTicks);

		for (Direction d : Iterate.horizontalDirections) {
			ms.push();
			CachedBufferer.partial(AllPartialModels.BOILER_GAUGE, blockState)
				.rotateY(d.asRotation())
				.unCentre()
				.translate(be.width / 2f - 6 / 16f, 0, 0)
				.light(light)
				.renderInto(ms, vb);
			CachedBufferer.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState)
				.rotateY(d.asRotation())
				.unCentre()
				.translate(be.width / 2f - 6 / 16f, 0, 0)
				.translate(0, dialPivot, dialPivot)
				.rotateX(-90 * progress)
				.translate(0, -dialPivot, -dialPivot)
				.light(light)
				.renderInto(ms, vb);
			ms.pop();
		}

		ms.pop();
	}

	@Override
	public boolean rendersOutsideBoundingBox(FluidTankBlockEntity be) {
		return be.isController();
	}

}

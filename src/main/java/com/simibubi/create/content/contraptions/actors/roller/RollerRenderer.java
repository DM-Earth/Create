package com.simibubi.create.content.contraptions.actors.roller;

import static net.minecraft.state.property.Properties.HORIZONTAL_FACING;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class RollerRenderer extends SmartBlockEntityRenderer<RollerBlockEntity> {

	public RollerRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RollerBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		BlockState blockState = be.getCachedState();

		ms.push();
		ms.translate(0, -0.25, 0);
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.ROLLER_WHEEL, blockState);
		Direction facing = blockState.get(RollerBlock.FACING);
		superBuffer.translate(Vec3d.of(facing.getVector())
			.multiply(17 / 16f));
		HarvesterRenderer.transform(be.getWorld(), facing, superBuffer, be.getAnimatedSpeed(), Vec3d.ZERO);
		superBuffer.translate(0, -.5, .5)
			.rotateY(90)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getCutoutMipped()));
		ms.pop();

		CachedBufferer.partial(AllPartialModels.ROLLER_FRAME, blockState)
			.rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing) + 180))
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getCutoutMipped()));
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffers) {
		BlockState blockState = context.state;
		Direction facing = blockState.get(HORIZONTAL_FACING);
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.ROLLER_WHEEL, blockState);
		float speed = (float) (!VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: -context.getAnimationSpeed());
		if (context.contraption.stalled)
			speed = 0;

		superBuffer.transform(matrices.getModel())
			.translate(Vec3d.of(facing.getVector())
				.multiply(17 / 16f));
		HarvesterRenderer.transform(context.world, facing, superBuffer, speed, Vec3d.ZERO);

		MatrixStack viewProjection = matrices.getViewProjection();
		viewProjection.push();
		viewProjection.translate(0, -.25, 0);
		int contraptionWorldLight = ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld);
		superBuffer.translate(0, -.5, .5)
			.rotateY(90)
			.light(matrices.getWorld(), contraptionWorldLight)
			.renderInto(viewProjection, buffers.getBuffer(RenderLayer.getCutoutMipped()));
		viewProjection.pop();

		CachedBufferer.partial(AllPartialModels.ROLLER_FRAME, blockState)
			.transform(matrices.getModel())
			.rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing) + 180))
			.light(matrices.getWorld(), contraptionWorldLight)
			.renderInto(viewProjection, buffers.getBuffer(RenderLayer.getCutoutMipped()));
	}

}

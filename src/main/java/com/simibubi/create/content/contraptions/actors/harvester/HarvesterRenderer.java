package com.simibubi.create.content.contraptions.actors.harvester;

import static net.minecraft.state.property.Properties.HORIZONTAL_FACING;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HarvesterRenderer extends SafeBlockEntityRenderer<HarvesterBlockEntity> {

	private static final Vec3d PIVOT = new Vec3d(0, 6, 9);

	public HarvesterRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(HarvesterBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.HARVESTER_BLADE, blockState);
		transform(be.getWorld(), blockState.get(HarvesterBlock.FACING), superBuffer, be.getAnimatedSpeed(), PIVOT);
		superBuffer.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getCutoutMipped()));
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, VertexConsumerProvider buffers) {
		BlockState blockState = context.state;
		Direction facing = blockState.get(HORIZONTAL_FACING);
		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.HARVESTER_BLADE, blockState);
		float speed = (float) (!VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite())
			? context.getAnimationSpeed()
			: 0);
		if (context.contraption.stalled)
			speed = 0;

		superBuffer.transform(matrices.getModel());
		transform(context.world, facing, superBuffer, speed, PIVOT);

		superBuffer
			.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld))
			.renderInto(matrices.getViewProjection(), buffers.getBuffer(RenderLayer.getCutoutMipped()));
	}

	public static void transform(World world, Direction facing, SuperByteBuffer superBuffer, float speed, Vec3d pivot) {
		float originOffset = 1 / 16f;
		Vec3d rotOffset = new Vec3d(0, pivot.y * originOffset, pivot.z * originOffset);
		float time = AnimationTickHolder.getRenderTime(world) / 20;
		float angle = (time * speed) % 360;

		superBuffer.rotateCentered(Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing)))
			.translate(rotOffset.x, rotOffset.y, rotOffset.z)
			.rotate(Direction.WEST, AngleHelper.rad(angle))
			.translate(-rotOffset.x, -rotOffset.y, -rotOffset.z);
	}
}

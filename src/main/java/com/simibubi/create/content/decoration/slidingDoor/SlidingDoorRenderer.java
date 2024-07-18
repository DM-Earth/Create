package com.simibubi.create.content.decoration.slidingDoor;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SlidingDoorRenderer extends SafeBlockEntityRenderer<SlidingDoorBlockEntity> {

	public SlidingDoorRenderer(Context context) {}

	@Override
	protected void renderSafe(SlidingDoorBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		if (!be.shouldRenderSpecial(blockState))
			return;

		Direction facing = blockState.get(DoorBlock.FACING);
		Direction movementDirection = facing.rotateYClockwise();

		if (blockState.get(DoorBlock.HINGE) == DoorHinge.LEFT)
			movementDirection = movementDirection.getOpposite();

		float value = be.animation.getValue(partialTicks);
		float value2 = MathHelper.clamp(value * 10, 0, 1);

		VertexConsumer vb = buffer.getBuffer(RenderLayer.getCutoutMipped());
		Vec3d offset = Vec3d.of(movementDirection.getVector())
			.multiply(value * value * 13 / 16f)
			.add(Vec3d.of(facing.getVector())
				.multiply(value2 * 1 / 32f));

		if (((SlidingDoorBlock) blockState.getBlock()).isFoldingDoor()) {
			Couple<PartialModel> partials =
				AllPartialModels.FOLDING_DOORS.get(Registries.BLOCK.getId(blockState.getBlock()));

			boolean flip = blockState.get(DoorBlock.HINGE) == DoorHinge.RIGHT;
			for (boolean left : Iterate.trueAndFalse) {
				SuperByteBuffer partial = CachedBufferer.partial(partials.get(left ^ flip), blockState);
				float f = flip ? -1 : 1;

				partial.translate(0, -1 / 512f, 0)
					.translate(Vec3d.of(facing.getVector())
						.multiply(value2 * 1 / 32f));
				partial.rotateCentered(Direction.UP,
					MathHelper.RADIANS_PER_DEGREE * AngleHelper.horizontalAngle(facing.rotateYClockwise()));

				if (flip)
					partial.translate(0, 0, 1);
				partial.rotateY(91 * f * value * value);

				if (!left)
					partial.translate(0, 0, f / 2f)
						.rotateY(-181 * f * value * value);

				if (flip)
					partial.translate(0, 0, -1 / 2f);

				partial.light(light)
					.renderInto(ms, vb);
			}

			return;
		}

		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			CachedBufferer.block(blockState.with(DoorBlock.OPEN, false)
				.with(DoorBlock.HALF, half))
				.translate(0, half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0, 0)
				.translate(offset)
				.light(light)
				.renderInto(ms, vb);
		}

	}

}

package com.simibubi.create.content.kinetics.transmission;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class SplitShaftRenderer extends KineticBlockEntityRenderer<SplitShaftBlockEntity> {

	public SplitShaftRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
			int light, int overlay) {
		if (Backend.canUseInstancing(be.getWorld())) return;

		Block block = be.getCachedState().getBlock();
		final Axis boxAxis = ((IRotate) block).getRotationAxis(be.getCachedState());
		final BlockPos pos = be.getPos();
		float time = AnimationTickHolder.getRenderTime(be.getWorld());

		for (Direction direction : Iterate.directions) {
			Axis axis = direction.getAxis();
			if (boxAxis != axis)
				continue;

			float offset = getRotationOffsetForPosition(be, pos, axis);
			float angle = (time * be.getSpeed() * 3f / 10) % 360;
			float modifier = be.getRotationSpeedModifier(direction);

			angle *= modifier;
			angle += offset;
			angle = angle / 180f * (float) Math.PI;

			SuperByteBuffer superByteBuffer =
					CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getCachedState(), direction);
			kineticRotationTransform(superByteBuffer, be, axis, angle, light);
			superByteBuffer.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
		}
	}

}

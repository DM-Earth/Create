package com.simibubi.create.content.kinetics.gearbox;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

public class GearboxRenderer extends KineticBlockEntityRenderer<GearboxBlockEntity> {

	public GearboxRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(GearboxBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
			int light, int overlay) {
		if (Backend.canUseInstancing(be.getWorld())) return;

		final Axis boxAxis = be.getCachedState().get(Properties.AXIS);
		final BlockPos pos = be.getPos();
		float time = AnimationTickHolder.getRenderTime(be.getWorld());

		for (Direction direction : Iterate.directions) {
			final Axis axis = direction.getAxis();
			if (boxAxis == axis)
				continue;

			SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getCachedState(), direction);
			float offset = getRotationOffsetForPosition(be, pos, axis);
			float angle = (time * be.getSpeed() * 3f / 10) % 360;

			if (be.getSpeed() != 0 && be.hasSource()) {
				BlockPos source = be.source.subtract(be.getPos());
				Direction sourceFacing = Direction.getFacing(source.getX(), source.getY(), source.getZ());
				if (sourceFacing.getAxis() == direction.getAxis())
					angle *= sourceFacing == direction ? 1 : -1;
				else if (sourceFacing.getDirection() == direction.getDirection())
					angle *= -1;
			}

			angle += offset;
			angle = angle / 180f * (float) Math.PI;

			kineticRotationTransform(shaft, be, axis, angle, light);
			shaft.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
		}
	}

}

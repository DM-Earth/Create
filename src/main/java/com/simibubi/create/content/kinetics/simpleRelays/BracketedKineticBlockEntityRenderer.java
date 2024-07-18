package com.simibubi.create.content.kinetics.simpleRelays;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;

public class BracketedKineticBlockEntityRenderer extends KineticBlockEntityRenderer<BracketedKineticBlockEntity> {

	public BracketedKineticBlockEntityRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BracketedKineticBlockEntity be, float partialTicks, MatrixStack ms,
		VertexConsumerProvider buffer, int light, int overlay) {

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		if (!AllBlocks.LARGE_COGWHEEL.has(be.getCachedState())) {
			super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
			return;
		}

		// Large cogs sometimes have to offset their teeth by 11.25 degrees in order to
		// mesh properly

		Axis axis = getRotationAxisOf(be);
		Direction facing = Direction.from(axis, AxisDirection.POSITIVE);
		renderRotatingBuffer(be,
			CachedBufferer.partialFacingVertical(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, be.getCachedState(), facing),
			ms, buffer.getBuffer(RenderLayer.getSolid()), light);

		float angle = getAngleForLargeCogShaft(be, axis);
		SuperByteBuffer shaft =
			CachedBufferer.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, be.getCachedState(), facing);
		kineticRotationTransform(shaft, be, axis, angle, light);
		shaft.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));

	}

	public static float getAngleForLargeCogShaft(SimpleKineticBlockEntity be, Axis axis) {
		BlockPos pos = be.getPos();
		float offset = getShaftAngleOffset(axis, pos);
		float time = AnimationTickHolder.getRenderTime(be.getWorld());
		float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
		return angle;
	}

	public static float getShaftAngleOffset(Axis axis, BlockPos pos) {
		float offset = 0;
		double d = (((axis == Axis.X) ? 0 : pos.getX()) + ((axis == Axis.Y) ? 0 : pos.getY())
			+ ((axis == Axis.Z) ? 0 : pos.getZ())) % 2;
		if (d == 0)
			offset = 22.5f;
		return offset;
	}

}

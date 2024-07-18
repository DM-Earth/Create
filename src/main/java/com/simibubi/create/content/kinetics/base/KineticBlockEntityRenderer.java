package com.simibubi.create.content.kinetics.base;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.render.SuperByteBufferCache;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;

public class KineticBlockEntityRenderer<T extends KineticBlockEntity> extends SafeBlockEntityRenderer<T> {

	public static final SuperByteBufferCache.Compartment<BlockState> KINETIC_BLOCK = new SuperByteBufferCache.Compartment<>();
	public static boolean rainbowMode = false;

	public KineticBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (Backend.canUseInstancing(be.getWorld())) return;

		BlockState state = getRenderedBlockState(be);
		RenderLayer type = getRenderType(be, state);
		if (type != null)
			renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
	}

	protected BlockState getRenderedBlockState(T be) {
		return be.getCachedState();
	}

	protected RenderLayer getRenderType(T be, BlockState state) {
		return RenderLayers.getBlockLayer(state);
	}

	protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
		return CachedBufferer.block(KINETIC_BLOCK, state);
	}

	public static void renderRotatingKineticBlock(KineticBlockEntity be, BlockState renderedState, MatrixStack ms,
		VertexConsumer buffer, int light) {
		SuperByteBuffer superByteBuffer = CachedBufferer.block(KINETIC_BLOCK, renderedState);
		renderRotatingBuffer(be, superByteBuffer, ms, buffer, light);
	}

	public static void renderRotatingBuffer(KineticBlockEntity be, SuperByteBuffer superBuffer, MatrixStack ms,
		VertexConsumer buffer, int light) {
		standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, buffer);
	}

	public static float getAngleForTe(KineticBlockEntity be, final BlockPos pos, Axis axis) {
		float time = AnimationTickHolder.getRenderTime(be.getWorld());
		float offset = getRotationOffsetForPosition(be, pos, axis);
		float angle = ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
		return angle;
	}

	public static SuperByteBuffer standardKineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be,
		int light) {
		final BlockPos pos = be.getPos();
		Axis axis = ((IRotate) be.getCachedState()
			.getBlock()).getRotationAxis(be.getCachedState());
		return kineticRotationTransform(buffer, be, axis, getAngleForTe(be, pos, axis), light);
	}

	public static SuperByteBuffer kineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be, Axis axis,
		float angle, int light) {
		buffer.light(light);
		buffer.rotateCentered(Direction.get(AxisDirection.POSITIVE, axis), angle);

		if (KineticDebugger.isActive()) {
			rainbowMode = true;
			buffer.color(be.hasNetwork() ? Color.generateFromLong(be.network) : Color.WHITE);
		} else {
			float overStressedEffect = be.effects.overStressedEffect;
			if (overStressedEffect != 0)
				if (overStressedEffect > 0)
					buffer.color(Color.WHITE.mixWith(Color.RED, overStressedEffect));
				else
					buffer.color(Color.WHITE.mixWith(Color.SPRING_GREEN, -overStressedEffect));
			else
				buffer.color(Color.WHITE);
		}

		return buffer;
	}

	public static float getRotationOffsetForPosition(KineticBlockEntity be, final BlockPos pos, final Axis axis) {
		float offset = ICogWheel.isLargeCog(be.getCachedState()) ? 11.25f : 0;
		double d = (((axis == Axis.X) ? 0 : pos.getX()) + ((axis == Axis.Y) ? 0 : pos.getY())
			+ ((axis == Axis.Z) ? 0 : pos.getZ())) % 2;
		if (d == 0)
			offset = 22.5f;
		return offset + be.getRotationAngleOffset(axis);
	}

	public static BlockState shaft(Axis axis) {
		return AllBlocks.SHAFT.getDefaultState()
			.with(Properties.AXIS, axis);
	}

	public static Axis getRotationAxisOf(KineticBlockEntity be) {
		return ((IRotate) be.getCachedState()
			.getBlock()).getRotationAxis(be.getCachedState());
	}

}

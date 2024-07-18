package com.simibubi.create.content.contraptions.pulley;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class AbstractPulleyRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

	private PartialModel halfRope;
	private PartialModel halfMagnet;

	public AbstractPulleyRenderer(BlockEntityRendererFactory.Context context, PartialModel halfRope,
		PartialModel halfMagnet) {
		super(context);
		this.halfRope = halfRope;
		this.halfMagnet = halfMagnet;
	}

	@Override
	public boolean rendersOutsideBoundingBox(T p_188185_1_) {
		return true;
	}

	@Override
	protected void renderSafe(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		float offset = getOffset(be, partialTicks);
		boolean running = isRunning(be);

		Axis rotationAxis = ((IRotate) be.getCachedState()
			.getBlock()).getRotationAxis(be.getCachedState());
		kineticRotationTransform(getRotatedCoil(be), be, rotationAxis, AngleHelper.rad(offset * 180), light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));

		World world = be.getWorld();
		BlockState blockState = be.getCachedState();
		BlockPos pos = be.getPos();

		SuperByteBuffer halfMagnet = CachedBufferer.partial(this.halfMagnet, blockState);
		SuperByteBuffer halfRope = CachedBufferer.partial(this.halfRope, blockState);
		SuperByteBuffer magnet = renderMagnet(be);
		SuperByteBuffer rope = renderRope(be);

		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		if (running || offset == 0)
			renderAt(world, offset > .25f ? magnet : halfMagnet, offset, pos, ms, vb);

		float f = offset % 1;
		if (offset > .75f && (f < .25f || f > .75f))
			renderAt(world, halfRope, f > .75f ? f - 1 : f, pos, ms, vb);

		if (!running)
			return;

		for (int i = 0; i < offset - 1.25f; i++)
			renderAt(world, rope, offset - i - 1, pos, ms, vb);
	}

	public static void renderAt(WorldAccess world, SuperByteBuffer partial, float offset, BlockPos pulleyPos,
		MatrixStack ms, VertexConsumer buffer) {
		BlockPos actualPos = pulleyPos.down((int) offset);
		int light = WorldRenderer.getLightmapCoordinates(world, world.getBlockState(actualPos), actualPos);
		partial.translate(0, -offset, 0)
			.light(light)
			.renderInto(ms, buffer);
	}

	protected abstract Axis getShaftAxis(T be);

	protected abstract PartialModel getCoil();

	protected abstract SuperByteBuffer renderRope(T be);

	protected abstract SuperByteBuffer renderMagnet(T be);

	protected abstract float getOffset(T be, float partialTicks);

	protected abstract boolean isRunning(T be);

	@Override
	protected BlockState getRenderedBlockState(T be) {
		return shaft(getShaftAxis(be));
	}

	protected SuperByteBuffer getRotatedCoil(T be) {
		BlockState blockState = be.getCachedState();
		return CachedBufferer.partialFacing(getCoil(), blockState,
			Direction.get(AxisDirection.POSITIVE, getShaftAxis(be)));
	}

	@Override
	public int getRenderDistance() {
		return 256;
	}

}

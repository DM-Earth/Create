package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ElevatorPulleyRenderer extends KineticBlockEntityRenderer<ElevatorPulleyBlockEntity> {

	public ElevatorPulleyRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ElevatorPulleyBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

//		if (Backend.canUseInstancing(be.getLevel()))
//			return;

		// from KBE. replace with super call when flw instance is implemented
		BlockState state = getRenderedBlockState(be);
		RenderLayer type = getRenderType(be, state);
		if (type != null)
			renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
		//

		float offset = PulleyRenderer.getBlockEntityOffset(partialTicks, be);
		boolean running = PulleyRenderer.isPulleyRunning(be);

		SpriteShiftEntry beltShift = AllSpriteShifts.ELEVATOR_BELT;
		SpriteShiftEntry coilShift = AllSpriteShifts.ELEVATOR_COIL;
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		World world = be.getWorld();
		BlockState blockState = be.getCachedState();
		BlockPos pos = be.getPos();

		float blockStateAngle =
			180 + AngleHelper.horizontalAngle(blockState.get(ElevatorPulleyBlock.HORIZONTAL_FACING));

		SuperByteBuffer magnet = CachedBufferer.partial(AllPartialModels.ELEVATOR_MAGNET, blockState);
		if (running || offset == 0)
			AbstractPulleyRenderer.renderAt(world, magnet.centre()
				.rotateY(blockStateAngle)
				.unCentre(), offset, pos, ms, vb);

		SuperByteBuffer rotatedCoil = getRotatedCoil(be);
		if (offset == 0) {
			rotatedCoil.light(light)
				.renderInto(ms, vb);
			return;
		}

		float spriteSize = beltShift.getTarget()
			.getMaxV()
			- beltShift.getTarget()
				.getMinV();

		double coilScroll = -(offset + 3 / 16f) - Math.floor((offset + 3 / 16f) * -2) / 2;
		double beltScroll = (-(offset + .5) - Math.floor(-(offset + .5))) / 2;

		rotatedCoil.shiftUVScrolling(coilShift, (float) coilScroll * spriteSize)
			.light(light)
			.renderInto(ms, vb);

		SuperByteBuffer halfRope = CachedBufferer.partial(AllPartialModels.ELEVATOR_BELT_HALF, blockState);
		SuperByteBuffer rope = CachedBufferer.partial(AllPartialModels.ELEVATOR_BELT, blockState);

		float f = offset % 1;
		if (f < .25f || f > .75f) {
			halfRope.centre()
				.rotateY(blockStateAngle)
				.unCentre();
			AbstractPulleyRenderer.renderAt(world,
				halfRope.shiftUVScrolling(beltShift, (float) beltScroll * spriteSize), f > .75f ? f - 1 : f, pos, ms,
				vb);
		}

		if (!running)
			return;

		for (int i = 0; i < offset - .25f; i++) {
			rope.centre()
				.rotateY(blockStateAngle)
				.unCentre();
			AbstractPulleyRenderer.renderAt(world, rope.shiftUVScrolling(beltShift, (float) beltScroll * spriteSize),
				offset - i, pos, ms, vb);
		}
	}

	@Override
	protected BlockState getRenderedBlockState(ElevatorPulleyBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	protected SuperByteBuffer getRotatedCoil(KineticBlockEntity be) {
		BlockState blockState = be.getCachedState();
		return CachedBufferer.partialFacing(AllPartialModels.ELEVATOR_COIL, blockState,
			blockState.get(ElevatorPulleyBlock.HORIZONTAL_FACING));
	}

	@Override
	public int getRenderDistance() {
		return 128;
	}

	@Override
	public boolean rendersOutsideBoundingBox(ElevatorPulleyBlockEntity p_188185_1_) {
		return true;
	}

}

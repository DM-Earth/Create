package com.simibubi.create.content.contraptions.pulley;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;

public class PulleyRenderer extends AbstractPulleyRenderer<PulleyBlockEntity> {

	public PulleyRenderer(BlockEntityRendererFactory.Context context) {
		super(context, AllPartialModels.ROPE_HALF, AllPartialModels.ROPE_HALF_MAGNET);
	}

	@Override
	protected Axis getShaftAxis(PulleyBlockEntity be) {
		return be.getCachedState()
			.get(PulleyBlock.HORIZONTAL_AXIS);
	}

	@Override
	protected PartialModel getCoil() {
		return AllPartialModels.ROPE_COIL;
	}

	@Override
	protected SuperByteBuffer renderRope(PulleyBlockEntity be) {
		return CachedBufferer.block(AllBlocks.ROPE.getDefaultState());
	}

	@Override
	protected SuperByteBuffer renderMagnet(PulleyBlockEntity be) {
		return CachedBufferer.block(AllBlocks.PULLEY_MAGNET.getDefaultState());
	}

	@Override
	protected float getOffset(PulleyBlockEntity be, float partialTicks) {
		return getBlockEntityOffset(partialTicks, be);
	}

	@Override
	protected boolean isRunning(PulleyBlockEntity be) {
		return isPulleyRunning(be);
	}

	public static boolean isPulleyRunning(PulleyBlockEntity be) {
		return be.running || be.mirrorParent != null || be.isVirtual();
	}

	public static float getBlockEntityOffset(float partialTicks, PulleyBlockEntity blockEntity) {
		float offset = blockEntity.getInterpolatedOffset(partialTicks);

		AbstractContraptionEntity attachedContraption = blockEntity.getAttachedContraption();
		if (attachedContraption != null) {
			PulleyContraption c = (PulleyContraption) attachedContraption.getContraption();
			double entityPos = MathHelper.lerp(partialTicks, attachedContraption.lastRenderY, attachedContraption.getY());
			offset = (float) -(entityPos - c.anchor.getY() - c.getInitialOffset());
		}

		return offset;
	}
	
	@Override
	public int getRenderDistance() {
		return 128;
	}
	
}

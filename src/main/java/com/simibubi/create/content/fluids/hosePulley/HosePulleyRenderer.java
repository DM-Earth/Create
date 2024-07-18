package com.simibubi.create.content.fluids.hosePulley;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.util.math.Direction.Axis;

public class HosePulleyRenderer extends AbstractPulleyRenderer<HosePulleyBlockEntity> {

	public HosePulleyRenderer(BlockEntityRendererFactory.Context context) {
		super(context, AllPartialModels.HOSE_HALF, AllPartialModels.HOSE_HALF_MAGNET);
	}

	@Override
	protected Axis getShaftAxis(HosePulleyBlockEntity be) {
		return be.getCachedState()
			.get(HosePulleyBlock.HORIZONTAL_FACING)
			.rotateYClockwise()
			.getAxis();
	}

	@Override
	protected PartialModel getCoil() {
		return AllPartialModels.HOSE_COIL;
	}

	@Override
	protected SuperByteBuffer renderRope(HosePulleyBlockEntity be) {
		return CachedBufferer.partial(AllPartialModels.HOSE, be.getCachedState());
	}

	@Override
	protected SuperByteBuffer renderMagnet(HosePulleyBlockEntity be) {
		return CachedBufferer.partial(AllPartialModels.HOSE_MAGNET, be.getCachedState());
	}

	@Override
	protected float getOffset(HosePulleyBlockEntity be, float partialTicks) {
		return be.getInterpolatedOffset(partialTicks);
	}

	@Override
	protected boolean isRunning(HosePulleyBlockEntity be) {
		return true;
	}

}

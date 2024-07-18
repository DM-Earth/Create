package com.simibubi.create.content.kinetics.motor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

public class CreativeMotorRenderer extends KineticBlockEntityRenderer<CreativeMotorBlockEntity> {

	public CreativeMotorRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(CreativeMotorBlockEntity be, BlockState state) {
		return CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, state);
	}

}

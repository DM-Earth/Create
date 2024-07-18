package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

public class MechanicalPistonRenderer extends KineticBlockEntityRenderer<MechanicalPistonBlockEntity> {

	public MechanicalPistonRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected BlockState getRenderedBlockState(MechanicalPistonBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}

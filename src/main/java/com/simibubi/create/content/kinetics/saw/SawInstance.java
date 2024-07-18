package com.simibubi.create.content.kinetics.saw;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

public class SawInstance extends SingleRotatingInstance<SawBlockEntity> {

	public SawInstance(MaterialManager materialManager, SawBlockEntity blockEntity) {
		super(materialManager, blockEntity);
	}

	@Override
	protected Instancer<RotatingData> getModel() {
		if (blockState.get(Properties.FACING)
			.getAxis()
			.isHorizontal()) {
			BlockState referenceState = blockState.rotate(BlockRotation.CLOCKWISE_180);
			Direction facing = referenceState.get(Properties.FACING);
			return getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, referenceState, facing);
		} else {
			return getRotatingMaterial().getModel(shaft());
		}
	}
}

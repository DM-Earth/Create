package com.simibubi.create.content.kinetics.transmission;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GearshiftBlockEntity extends SplitShaftBlockEntity {

	public GearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public float getRotationSpeedModifier(Direction face) {
		if (hasSource()) {
			if (face != getSourceFacing() && getCachedState().get(Properties.POWERED))
				return -1;
		}
		return 1;
	}
	
}

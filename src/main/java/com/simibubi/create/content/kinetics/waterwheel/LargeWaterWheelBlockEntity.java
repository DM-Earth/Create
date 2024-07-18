package com.simibubi.create.content.kinetics.waterwheel;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class LargeWaterWheelBlockEntity extends WaterWheelBlockEntity {

	public LargeWaterWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected int getSize() {
		return 2;
	}

}

package com.simibubi.create.content.kinetics.gearbox;

import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class GearboxBlockEntity extends DirectionalShaftHalvesBlockEntity {

	public GearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	@Override
	protected boolean isNoisy() {
		return false;
	}
	
}

package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class DrillBlockEntity extends BlockBreakingKineticBlockEntity {

	public DrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected BlockPos getBreakingPos() {
		return getPos().offset(getCachedState().get(DrillBlock.FACING));
	}

}

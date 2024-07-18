package com.simibubi.create.content.redstone.diodes;

import static com.simibubi.create.content.redstone.diodes.BrassDiodeBlock.POWERING;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class PulseExtenderBlockEntity extends BrassDiodeBlockEntity {

	public PulseExtenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin) {
		if (atMin && !powered)
			return;
		if (atMin || powered) {
			world.setBlockState(pos, getCachedState().with(POWERING, true));
			state = maxState.getValue();
			return;
		}
		
		if (state == 1) {
			if (powering && !world.isClient)
				world.setBlockState(pos, getCachedState().with(POWERING, false));
			if (!powered)
				state = 0;
			return;
		}
		
		if (!powered)
			state--;
	}
}

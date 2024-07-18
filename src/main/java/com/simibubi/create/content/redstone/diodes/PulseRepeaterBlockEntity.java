package com.simibubi.create.content.redstone.diodes;

import static com.simibubi.create.content.redstone.diodes.BrassDiodeBlock.POWERING;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class PulseRepeaterBlockEntity extends BrassDiodeBlockEntity {

	public PulseRepeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin) {
		if (atMin && !powered)
			return;
		if (state > maxState.getValue() + 1) {
			if (!powered && !powering)
				state = 0;
			return;
		}

		state++;
		if (world.isClient)
			return;

		if (state == maxState.getValue() - 1 && !powering)
			world.setBlockState(pos, getCachedState().cycle(POWERING));
		if (state == maxState.getValue() + 1 && powering)
			world.setBlockState(pos, getCachedState().cycle(POWERING));
	}

}

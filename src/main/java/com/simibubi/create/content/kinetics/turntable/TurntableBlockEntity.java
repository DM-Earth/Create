package com.simibubi.create.content.kinetics.turntable;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class TurntableBlockEntity extends KineticBlockEntity {

	public TurntableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

}

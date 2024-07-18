package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.AllBogeyStyles;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class StandardBogeyBlockEntity extends AbstractBogeyBlockEntity {

	public StandardBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public BogeyStyle getDefaultStyle() {
		return AllBogeyStyles.STANDARD;
	}
}

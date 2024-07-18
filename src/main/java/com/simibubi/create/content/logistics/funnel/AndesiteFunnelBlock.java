package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.AllBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public class AndesiteFunnelBlock extends FunnelBlock {

	public AndesiteFunnelBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
	}

	@Override
	public BlockState getEquivalentBeltFunnel(BlockView world, BlockPos pos, BlockState state) {
		Direction facing = getFunnelFacing(state);
		return AllBlocks.ANDESITE_BELT_FUNNEL.getDefaultState()
			.with(BeltFunnelBlock.HORIZONTAL_FACING, facing)
			.with(POWERED, state.get(POWERED));
	}

}

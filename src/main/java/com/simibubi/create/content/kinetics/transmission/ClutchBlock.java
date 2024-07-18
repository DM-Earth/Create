package com.simibubi.create.content.kinetics.transmission;

import com.simibubi.create.AllBlockEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClutchBlock extends GearshiftBlock {

	public ClutchBlock(Settings properties) {
		super(properties);
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		if (worldIn.isClient)
			return;

		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != worldIn.isReceivingRedstonePower(pos)) {
			worldIn.setBlockState(pos, state.cycle(POWERED), 2 | 16);
			detachKinetics(worldIn, pos, previouslyPowered);
		}
	}
	
	@Override
	public BlockEntityType<? extends SplitShaftBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CLUTCH.get();
	}

}

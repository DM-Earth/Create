package com.simibubi.create.content.decoration.palettes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

public class WindowBlock extends ConnectedGlassBlock {

	protected final boolean translucent;

	public WindowBlock(Settings p_i48392_1_, boolean translucent) {
		super(p_i48392_1_);
		this.translucent = translucent;
	}

	public boolean isTranslucent() {
		return translucent;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
		if (state.getBlock() == adjacentBlockState.getBlock()) {
			return true;
		}
		if (state.getBlock() instanceof WindowBlock windowBlock
				&& adjacentBlockState.getBlock() instanceof ConnectedGlassBlock) {
			return !windowBlock.isTranslucent() && side.getAxis().isHorizontal();
		}
		return super.isSideInvisible(state, adjacentBlockState, side);
	}

}

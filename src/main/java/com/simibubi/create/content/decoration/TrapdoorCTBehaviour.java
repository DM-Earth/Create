package com.simibubi.create.content.decoration;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class TrapdoorCTBehaviour extends ConnectedTextureBehaviour.Base {

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable Sprite sprite) {
		return AllSpriteShifts.FRAMED_GLASS;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockRenderView reader, BlockPos pos,
		BlockPos otherPos, Direction face, Direction primaryOffset, Direction secondaryOffset) {
		return state.getBlock() == other.getBlock()
			&& TrainTrapdoorBlock.isConnected(state, other, primaryOffset == null ? secondaryOffset : primaryOffset);
	}

}

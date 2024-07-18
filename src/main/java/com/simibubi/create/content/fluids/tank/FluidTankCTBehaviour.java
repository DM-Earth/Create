package com.simibubi.create.content.fluids.tank;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockRenderView;

public class FluidTankCTBehaviour extends HorizontalCTBehaviour {

	private CTSpriteShiftEntry innerShift;

	public FluidTankCTBehaviour(CTSpriteShiftEntry layerShift, CTSpriteShiftEntry topShift, CTSpriteShiftEntry innerShift) {
		super(layerShift, topShift);
		this.innerShift = innerShift;
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable Sprite sprite) {
		if (sprite != null && direction.getAxis() == Axis.Y && innerShift.getOriginal() == sprite)
			return innerShift;
		return super.getShift(state, direction, sprite);
	}

	public boolean buildContextForOccludedDirections() {
		return true;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockRenderView reader, BlockPos pos, BlockPos otherPos,
		Direction face) {
		return state.getBlock() == other.getBlock() && ConnectivityHandler.isConnected(reader, pos, otherPos);
	}
}

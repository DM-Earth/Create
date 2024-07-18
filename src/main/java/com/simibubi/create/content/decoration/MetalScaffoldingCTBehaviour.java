package com.simibubi.create.content.decoration;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockRenderView;

public class MetalScaffoldingCTBehaviour extends HorizontalCTBehaviour {

	protected CTSpriteShiftEntry insideShift;

	public MetalScaffoldingCTBehaviour(CTSpriteShiftEntry outsideShift, CTSpriteShiftEntry insideShift,
		CTSpriteShiftEntry topShift) {
		super(outsideShift, topShift);
		this.insideShift = insideShift;
	}

	@Override
	public boolean buildContextForOccludedDirections() {
		return true;
	}

	@Override
	protected boolean isBeingBlocked(BlockState state, BlockRenderView reader, BlockPos pos, BlockPos otherPos,
		Direction face) {
		return face.getAxis() == Axis.Y && super.isBeingBlocked(state, reader, pos, otherPos, face);
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable Sprite sprite) {
		if (direction.getAxis() != Axis.Y && sprite == insideShift.getOriginal())
			return insideShift;
		return super.getShift(state, direction, sprite);
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockRenderView reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return super.connectsTo(state, other, reader, pos, otherPos, face)
			&& state.get(MetalScaffoldingBlock.BOTTOM) && other.get(MetalScaffoldingBlock.BOTTOM);
	}

}

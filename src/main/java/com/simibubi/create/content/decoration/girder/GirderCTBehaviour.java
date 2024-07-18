package com.simibubi.create.content.decoration.girder;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.BlockRenderView;

public class GirderCTBehaviour extends ConnectedTextureBehaviour.Base {

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable Sprite sprite) {
		if (!state.contains(GirderBlock.X))
			return null;
		return !state.get(GirderBlock.X) && !state.get(GirderBlock.Z) && direction.getAxis() != Axis.Y
			? AllSpriteShifts.GIRDER_POLE
			: null;
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockRenderView reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		if (other.getBlock() != state.getBlock())
			return false;
		return !other.get(GirderBlock.X) && !other.get(GirderBlock.Z);
	}

}

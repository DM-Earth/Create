package com.simibubi.create.content.kinetics.simpleRelays;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

public class SimpleKineticBlockEntity extends KineticBlockEntity {

	public SimpleKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos).expand(1);
	}

	@Override
	public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
		if (!ICogWheel.isLargeCog(state))
			return super.addPropagationLocations(block, state, neighbours);

		BlockPos.stream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
			.forEach(offset -> {
				if (offset.getSquaredDistance(BlockPos.ORIGIN) == 2)
					neighbours.add(pos.add(offset));
			});
		return neighbours;
	}

	@Override
	protected boolean isNoisy() {
		return false;
	}

}

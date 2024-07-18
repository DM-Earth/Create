package com.simibubi.create.content.trains.track;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class FakeTrackBlockEntity extends SyncedBlockEntity {

	int keepAlive;
	
	public FakeTrackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		keepAlive();
	}
	
	public void randomTick() {
		keepAlive--;
		if (keepAlive > 0)
			return;
		world.removeBlock(pos, false);
	}
	
	public void keepAlive() {
		keepAlive = 3;
	}
	

}

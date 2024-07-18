package com.simibubi.create.content.contraptions.actors.harvester;

import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class HarvesterBlockEntity extends CachedRenderBBBlockEntity {

	// For simulations such as Ponder
	private float manuallyAnimatedSpeed;

	public HarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return new Box(pos);
	}

	public float getAnimatedSpeed() {
		return manuallyAnimatedSpeed;
	}

	public void setAnimatedSpeed(float speed) {
		manuallyAnimatedSpeed = speed;
	}

}

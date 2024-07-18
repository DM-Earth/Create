package com.simibubi.create.content.decoration.slidingDoor;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

public class SlidingDoorBlockEntity extends SmartBlockEntity {

	LerpedFloat animation;
	int bridgeTicks;
	boolean deferUpdate;

	public SlidingDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		animation = LerpedFloat.linear()
			.startWithValue(isOpen(state) ? 1 : 0);
	}

	@Override
	public void tick() {
		if (deferUpdate && !world.isClient()) {
			deferUpdate = false;
			BlockState blockState = getCachedState();
			blockState.neighborUpdate(world, pos, Blocks.AIR, pos, false);
		}

		super.tick();
		boolean open = isOpen(getCachedState());
		boolean wasSettled = animation.settled();
		animation.chase(open ? 1 : 0, .15f, Chaser.LINEAR);
		animation.tickChaser();

		if (world.isClient()) {
			if (bridgeTicks < 2 && open)
				bridgeTicks++;
			else if (bridgeTicks > 0 && !open && isVisible(getCachedState()))
				bridgeTicks--;
			return;
		}

		if (!open && !wasSettled && animation.settled() && !isVisible(getCachedState()))
			showBlockModel();
	}

	@Override
	protected Box createRenderBoundingBox() {
		return super.createRenderBoundingBox().expand(1);
	}

	protected boolean isVisible(BlockState state) {
		return state.getOrEmpty(SlidingDoorBlock.VISIBLE)
			.orElse(true);
	}

	protected boolean shouldRenderSpecial(BlockState state) {
		return !isVisible(state) || bridgeTicks != 0;
	}

	protected void showBlockModel() {
		world.setBlockState(pos, getCachedState().with(SlidingDoorBlock.VISIBLE, true), 3);
		world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, .5f, 1);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public static boolean isOpen(BlockState state) {
		return state.getOrEmpty(DoorBlock.OPEN)
			.orElse(false);
	}

}

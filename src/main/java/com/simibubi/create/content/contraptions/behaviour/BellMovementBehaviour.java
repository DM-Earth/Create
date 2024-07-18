package com.simibubi.create.content.contraptions.behaviour;

import com.simibubi.create.content.equipment.bell.AbstractBellBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.minecraft.block.Block;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BellMovementBehaviour implements MovementBehaviour {

	@Override
	public boolean renderAsNormalBlockEntity() {
		return true;
	}
	
	@Override
	public boolean isActive(MovementContext context) {
		return MovementBehaviour.super.isActive(context) && !(context.contraption instanceof CarriageContraption);
	}

	@Override
	public void onSpeedChanged(MovementContext context, Vec3d oldMotion, Vec3d motion) {
		double dotProduct = oldMotion.dotProduct(motion);

		if (dotProduct <= 0 && (context.relativeMotion.length() != 0) || context.firstMovement)
			playSound(context);
	}

	@Override
	public void stopMoving(MovementContext context) {
		if (context.position != null && isActive(context))
			playSound(context);
	}

	public static void playSound(MovementContext context) {
		World world = context.world;
		BlockPos pos = BlockPos.ofFloored(context.position);
		Block block = context.state.getBlock();

		if (block instanceof AbstractBellBlock) {
			((AbstractBellBlock<?>) block).playSound(world, pos);
		} else {
			// Vanilla bell sound
			world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE,
					SoundCategory.BLOCKS, 2f, 1f);
		}
	}
}

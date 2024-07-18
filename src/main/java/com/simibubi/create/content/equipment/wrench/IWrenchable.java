package com.simibubi.create.content.equipment.wrench;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface IWrenchable {

	default ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		BlockState rotated = getRotatedBlockState(state, context.getSide());
		if (!rotated.canPlaceAt(world, context.getBlockPos()))
			return ActionResult.PASS;

		KineticBlockEntity.switchToBlockState(world, context.getBlockPos(), updateAfterWrenched(rotated, context));

		BlockEntity be = context.getWorld()
			.getBlockEntity(context.getBlockPos());
		if (be instanceof GeneratingKineticBlockEntity) {
			((GeneratingKineticBlockEntity) be).reActivateSource = true;
		}

		if (world.getBlockState(context.getBlockPos()) != state)
			playRotateSound(world, context.getBlockPos());

		return ActionResult.SUCCESS;
	}

	default BlockState updateAfterWrenched(BlockState newState, ItemUsageContext context) {
//		return newState;
		return Block.postProcessState(newState, context.getWorld(), context.getBlockPos());
	}

	default ActionResult onSneakWrenched(BlockState state, ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		if (world instanceof ServerWorld) {
			if (player != null && !player.isCreative())
				Block.getDroppedStacks(state, (ServerWorld) world, pos, world.getBlockEntity(pos), player, context.getStack())
					.forEach(itemStack -> {
						player.getInventory().offerOrDrop(itemStack);
					});
			state.onStacksDropped((ServerWorld) world, pos, ItemStack.EMPTY, true);
			world.breakBlock(pos, false);
			playRemoveSound(world, pos);
		}
		return ActionResult.SUCCESS;
	}

	default void playRemoveSound(World world, BlockPos pos) {
		AllSoundEvents.WRENCH_REMOVE.playOnServer(world, pos, 1, Create.RANDOM.nextFloat() * .5f + .5f);
	}

	default void playRotateSound(World world, BlockPos pos) {
		AllSoundEvents.WRENCH_ROTATE.playOnServer(world, pos, 1, Create.RANDOM.nextFloat() + .5f);
	}

	default BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		BlockState newState = originalState;

		if (targetedFace.getAxis() == Direction.Axis.Y) {
			if (originalState.contains(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))
				return originalState.with(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, VoxelShaper
					.axisAsFace(originalState.get(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))
					.rotateClockwise(targetedFace.getAxis())
					.getAxis());
			if (originalState.contains(HorizontalKineticBlock.HORIZONTAL_FACING))
				return originalState.with(HorizontalKineticBlock.HORIZONTAL_FACING, originalState
					.get(HorizontalKineticBlock.HORIZONTAL_FACING).rotateClockwise(targetedFace.getAxis()));
		}

		if (originalState.contains(RotatedPillarKineticBlock.AXIS))
			return originalState.with(RotatedPillarKineticBlock.AXIS,
				VoxelShaper
					.axisAsFace(originalState.get(RotatedPillarKineticBlock.AXIS))
					.rotateClockwise(targetedFace.getAxis())
					.getAxis());

		if (!originalState.contains(DirectionalKineticBlock.FACING))
			return originalState;

		Direction stateFacing = originalState.get(DirectionalKineticBlock.FACING);

		if (stateFacing.getAxis()
			.equals(targetedFace.getAxis())) {
			if (originalState.contains(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE))
				return originalState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
			else
				return originalState;
		} else {
			do {
				newState = newState.with(DirectionalKineticBlock.FACING,
					newState.get(DirectionalKineticBlock.FACING).rotateClockwise(targetedFace.getAxis()));
				if (targetedFace.getAxis() == Direction.Axis.Y
					&& newState.contains(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE))
					newState = newState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
			} while (newState.get(DirectionalKineticBlock.FACING)
				.getAxis()
				.equals(targetedFace.getAxis()));
		}
		return newState;
	}
}

package com.simibubi.create.content.redstone.contact;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.elevator.ElevatorContraption;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.tick.TickPriority;

public class ContactMovementBehaviour implements MovementBehaviour {

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return Vec3d.of(context.state.get(RedstoneContactBlock.FACING)
			.getVector())
			.multiply(.65f);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		BlockState block = context.state;
		World world = context.world;

		if (world.isClient)
			return;
		if (context.firstMovement)
			return;

		deactivateLastVisitedContact(context);
		BlockState visitedState = world.getBlockState(pos);
		if (!AllBlocks.REDSTONE_CONTACT.has(visitedState) && !AllBlocks.ELEVATOR_CONTACT.has(visitedState))
			return;

		Vec3d contact = Vec3d.of(block.get(RedstoneContactBlock.FACING)
			.getVector());
		contact = context.rotation.apply(contact);
		Direction direction = Direction.getFacing(contact.x, contact.y, contact.z);

		if (visitedState.get(RedstoneContactBlock.FACING) != direction.getOpposite())
			return;

		if (AllBlocks.REDSTONE_CONTACT.has(visitedState))
			world.setBlockState(pos, visitedState.with(RedstoneContactBlock.POWERED, true));
		if (AllBlocks.ELEVATOR_CONTACT.has(visitedState) && context.contraption instanceof ElevatorContraption ec) 
			ec.broadcastFloorData(world, pos);

		context.data.put("lastContact", NbtHelper.fromBlockPos(pos));
		return;
	}

	@Override
	public void stopMoving(MovementContext context) {
		deactivateLastVisitedContact(context);
	}

	@Override
	public void cancelStall(MovementContext context) {
		MovementBehaviour.super.cancelStall(context);
		deactivateLastVisitedContact(context);
	}

	public void deactivateLastVisitedContact(MovementContext context) {
		if (!context.data.contains("lastContact"))
			return;

		BlockPos last = NbtHelper.toBlockPos(context.data.getCompound("lastContact"));
		context.data.remove("lastContact");
		BlockState blockState = context.world.getBlockState(last);

		if (AllBlocks.REDSTONE_CONTACT.has(blockState))
			context.world.scheduleBlockTick(last, AllBlocks.REDSTONE_CONTACT.get(), 1, TickPriority.NORMAL);
	}

}

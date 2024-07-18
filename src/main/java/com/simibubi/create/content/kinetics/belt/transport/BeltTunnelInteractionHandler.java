package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.source.AccumulatedItemCountDisplaySource;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BeltTunnelInteractionHandler {

	public static boolean flapTunnelsAndCheckIfStuck(BeltInventory beltInventory, TransportedItemStack current,
		float nextOffset) {

		int currentSegment = (int) current.beltPosition;
		int upcomingSegment = (int) nextOffset;

		Direction movementFacing = beltInventory.belt.getMovementFacing();
		if (!beltInventory.beltMovementPositive && nextOffset == 0)
			upcomingSegment = -1;
		if (currentSegment == upcomingSegment)
			return false;

		if (stuckAtTunnel(beltInventory, upcomingSegment, current.stack, movementFacing)) {
			current.beltPosition = currentSegment + (beltInventory.beltMovementPositive ? .99f : .01f);
			return true;
		}

		World world = beltInventory.belt.getWorld();
		boolean onServer = !world.isClient || beltInventory.belt.isVirtual();
		boolean removed = false;
		BeltTunnelBlockEntity nextTunnel = getTunnelOnSegment(beltInventory, upcomingSegment);
		int transferred = current.stack.getCount();

		if (nextTunnel instanceof BrassTunnelBlockEntity) {
			BrassTunnelBlockEntity brassTunnel = (BrassTunnelBlockEntity) nextTunnel;
			if (brassTunnel.hasDistributionBehaviour()) {
				if (!brassTunnel.canTakeItems())
					return true;
				if (onServer) {
					brassTunnel.setStackToDistribute(current.stack, movementFacing.getOpposite(), null);
					current.stack = ItemStack.EMPTY;
					beltInventory.belt.sendData();
					beltInventory.belt.markDirty();
				}
				removed = true;
			}
		} else if (nextTunnel != null) {
			BlockState blockState = nextTunnel.getCachedState();
			if (current.stack.getCount() > 1 && AllBlocks.ANDESITE_TUNNEL.has(blockState)
				&& BeltTunnelBlock.isJunction(blockState)
				&& movementFacing.getAxis() == blockState.get(BeltTunnelBlock.HORIZONTAL_AXIS)) {

				for (Direction d : Iterate.horizontalDirections) {
					if (d.getAxis() == blockState.get(BeltTunnelBlock.HORIZONTAL_AXIS))
						continue;
					if (!nextTunnel.flaps.containsKey(d))
						continue;
					BlockPos outpos = nextTunnel.getPos()
						.down()
						.offset(d);
					if (!world.canSetBlock(outpos))
						return true;
					DirectBeltInputBehaviour behaviour =
						BlockEntityBehaviour.get(world, outpos, DirectBeltInputBehaviour.TYPE);
					if (behaviour == null)
						continue;
					if (!behaviour.canInsertFromSide(d))
						continue;

					ItemStack toinsert = ItemHandlerHelper.copyStackWithSize(current.stack, 1);
					if (!behaviour.handleInsertion(toinsert, d, false)
						.isEmpty())
						return true;
					if (onServer)
						flapTunnel(beltInventory, upcomingSegment, d, false);

					current.stack.decrement(1);
					beltInventory.belt.sendData();
					if (current.stack.getCount() <= 1)
						break;
				}
			}
		}

		if (onServer) {
			flapTunnel(beltInventory, currentSegment, movementFacing, false);
			flapTunnel(beltInventory, upcomingSegment, movementFacing.getOpposite(), true);

			if (nextTunnel != null)
				DisplayLinkBlock.sendToGatherers(world, nextTunnel.getPos(),
					(dgte, b) -> b.itemReceived(dgte, transferred), AccumulatedItemCountDisplaySource.class);
		}

		if (removed)
			return true;

		return false;
	}

	public static boolean stuckAtTunnel(BeltInventory beltInventory, int offset, ItemStack stack,
		Direction movementDirection) {
		BeltBlockEntity belt = beltInventory.belt;
		BlockPos pos = BeltHelper.getPositionForOffset(belt, offset)
			.up();
		if (!(belt.getWorld()
			.getBlockState(pos)
			.getBlock() instanceof BrassTunnelBlock))
			return false;
		BlockEntity be = belt.getWorld()
			.getBlockEntity(pos);
		if (be == null || !(be instanceof BrassTunnelBlockEntity))
			return false;
		BrassTunnelBlockEntity tunnel = (BrassTunnelBlockEntity) be;
		return !tunnel.canInsert(movementDirection.getOpposite(), stack);
	}

	public static void flapTunnel(BeltInventory beltInventory, int offset, Direction side, boolean inward) {
		BeltTunnelBlockEntity be = getTunnelOnSegment(beltInventory, offset);
		if (be == null)
			return;
		be.flap(side, inward);
	}

	protected static BeltTunnelBlockEntity getTunnelOnSegment(BeltInventory beltInventory, int offset) {
		BeltBlockEntity belt = beltInventory.belt;
		if (belt.getCachedState()
			.get(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL)
			return null;
		return getTunnelOnPosition(belt.getWorld(), BeltHelper.getPositionForOffset(belt, offset));
	}

	public static BeltTunnelBlockEntity getTunnelOnPosition(World world, BlockPos pos) {
		pos = pos.up();
		if (!(world.getBlockState(pos)
			.getBlock() instanceof BeltTunnelBlock))
			return null;
		BlockEntity be = world.getBlockEntity(pos);
		if (be == null || !(be instanceof BeltTunnelBlockEntity))
			return null;
		return ((BeltTunnelBlockEntity) be);
	}

}

package com.simibubi.create.content.contraptions.behaviour.dispenser;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class MovedDefaultDispenseItemBehaviour implements IMovedDispenseItemBehaviour {
	private static final MovedDefaultDispenseItemBehaviour DEFAULT_INSTANCE = new MovedDefaultDispenseItemBehaviour();

	public static void doDispense(World p_82486_0_, ItemStack p_82486_1_, int p_82486_2_, Vec3d facing,
		BlockPos p_82486_4_, MovementContext context) {
		double d0 = p_82486_4_.getX() + facing.x + .5;
		double d1 = p_82486_4_.getY() + facing.y + .5;
		double d2 = p_82486_4_.getZ() + facing.z + .5;
		if (Direction.getFacing(facing.x, facing.y, facing.z)
			.getAxis() == Direction.Axis.Y) {
			d1 = d1 - 0.125D;
		} else {
			d1 = d1 - 0.15625D;
		}

		ItemEntity itementity = new ItemEntity(p_82486_0_, d0, d1, d2, p_82486_1_);
		double d3 = p_82486_0_.random.nextDouble() * 0.1D + 0.2D;
		itementity.setVelocity(
			p_82486_0_.random.nextGaussian() * (double) 0.0075F * (double) p_82486_2_ + facing.getX() * d3
				+ context.motion.x,
			p_82486_0_.random.nextGaussian() * (double) 0.0075F * (double) p_82486_2_ + facing.getY() * d3
				+ context.motion.y,
			p_82486_0_.random.nextGaussian() * (double) 0.0075F * (double) p_82486_2_ + facing.getZ() * d3
				+ context.motion.z);
		p_82486_0_.spawnEntity(itementity);
	}

	@Override
	public ItemStack dispense(ItemStack itemStack, MovementContext context, BlockPos pos) {
		Vec3d facingVec = Vec3d.of(context.state.get(DispenserBlock.FACING)
			.getVector());
		facingVec = context.rotation.apply(facingVec);
		facingVec.normalize();

		Direction closestToFacing = getClosestFacingDirection(facingVec);
		Inventory inventory = HopperBlockEntity.getInventoryAt(context.world, pos.offset(closestToFacing));
		if (inventory == null) {
			this.playDispenseSound(context.world, pos);
			this.spawnDispenseParticles(context.world, pos, closestToFacing);
			return this.dispenseStack(itemStack, context, pos, facingVec);
		} else {
			if (HopperBlockEntity.transfer(null, inventory, itemStack.copy()
				.split(1), closestToFacing.getOpposite())
				.isEmpty())
				itemStack.decrement(1);
			return itemStack;
		}
	}

	/**
	 * Dispense the specified stack, play the dispense sound and spawn particles.
	 */
	protected ItemStack dispenseStack(ItemStack itemStack, MovementContext context, BlockPos pos, Vec3d facing) {
		ItemStack itemstack = itemStack.split(1);
		doDispense(context.world, itemstack, 6, facing, pos, context);
		return itemStack;
	}

	/**
	 * Play the dispense sound from the specified block.
	 */
	protected void playDispenseSound(WorldAccess world, BlockPos pos) {
		world.syncWorldEvent(1000, pos, 0);
	}

	/**
	 * Order clients to display dispense particles from the specified block and
	 * facing.
	 */
	protected void spawnDispenseParticles(WorldAccess world, BlockPos pos, Vec3d facing) {
		spawnDispenseParticles(world, pos, getClosestFacingDirection(facing));
	}

	protected void spawnDispenseParticles(WorldAccess world, BlockPos pos, Direction direction) {
		world.syncWorldEvent(2000, pos, direction.getId());
	}

	protected Direction getClosestFacingDirection(Vec3d exactFacing) {
		return Direction.getFacing(exactFacing.x, exactFacing.y, exactFacing.z);
	}

	protected ItemStack placeItemInInventory(ItemStack consumedFrom, ItemStack output, MovementContext context,
		BlockPos pos, Vec3d facing) {
		consumedFrom.decrement(1);
		try (Transaction t = TransferUtil.getTransaction()) {
			long inserted = TransferUtil.insertItem(context.contraption.getSharedInventory(), output.copy());
			if (inserted != 0)
				DEFAULT_INSTANCE.dispenseStack(output, context, pos, facing);
			t.commit();
			return consumedFrom;
		}
	}
}

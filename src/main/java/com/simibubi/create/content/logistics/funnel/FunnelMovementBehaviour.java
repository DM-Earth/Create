package com.simibubi.create.content.logistics.funnel;

import java.util.List;

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FunnelMovementBehaviour implements MovementBehaviour {

	private final boolean hasFilter;

	public static FunnelMovementBehaviour andesite() {
		return new FunnelMovementBehaviour(false);
	}

	public static FunnelMovementBehaviour brass() {
		return new FunnelMovementBehaviour(true);
	}

	private FunnelMovementBehaviour(boolean hasFilter) {
		this.hasFilter = hasFilter;
	}

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		Direction facing = FunnelBlock.getFunnelFacing(context.state);
		Vec3d vec = Vec3d.of(facing.getVector());
		if (facing != Direction.UP)
			return vec.multiply(context.state.get(FunnelBlock.EXTRACTING) ? .15 : .65);

		return vec.multiply(.65);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		MovementBehaviour.super.visitNewPosition(context, pos);

		if (context.state.get(FunnelBlock.EXTRACTING))
			extract(context, pos);
		else
			succ(context, pos);

	}

	private void extract(MovementContext context, BlockPos pos) {
		World world = context.world;

		Vec3d entityPos = context.position;
		if (context.state.get(FunnelBlock.FACING) != Direction.DOWN)
			entityPos = entityPos.add(0, -.5f, 0);

		if (!world.getBlockState(pos)
			.getCollisionShape(world, pos)
			.isEmpty())
			return;

		if (!world.getNonSpectatingEntities(ItemEntity.class, new Box(BlockPos.ofFloored(entityPos)))
			.isEmpty())
			return;

		FilterItemStack filter = context.getFilterFromBE();
		int filterAmount = context.blockEntityData.getInt("FilterAmount");
		boolean upTo = context.blockEntityData.getBoolean("UpTo");
		if (filterAmount <= 0)
			filterAmount = hasFilter ? 64 : 1;

		ItemStack extract = ItemHelper.extract(context.contraption.getSharedInventory(),
			s -> filter.test(world, s),
			upTo ? ItemHelper.ExtractionCountMode.UPTO : ItemHelper.ExtractionCountMode.EXACTLY, filterAmount, false);

		if (extract.isEmpty())
			return;

		if (world.isClient)
			return;

		ItemEntity entity = new ItemEntity(world, entityPos.x, entityPos.y, entityPos.z, extract);
		entity.setVelocity(Vec3d.ZERO);
		entity.setPickupDelay(5);
		world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1 / 16f, .1f);
		world.spawnEntity(entity);
	}

	private void succ(MovementContext context, BlockPos pos) {
		World world = context.world;
		List<ItemEntity> items = world.getNonSpectatingEntities(ItemEntity.class, new Box(pos));
		FilterItemStack filter = context.getFilterFromBE();

		try (Transaction t = TransferUtil.getTransaction()) {
			for (ItemEntity item : items) {
				if (!item.isAlive())
					continue;
				ItemStack toInsert = item.getStack();
				if (toInsert.isEmpty() || (!filter.test(context.world, toInsert)))
					continue;
				long inserted = TransferUtil.insertItem(context.contraption.getSharedInventory(), toInsert);
				if (inserted == 0)
					continue;
				if (inserted == toInsert.getCount()) {
					item.setStack(ItemStack.EMPTY);
					item.discard();
					continue;
				}
				ItemStack remainder = item.getStack().copy();
				remainder.decrement(ItemHelper.truncateLong(inserted));
				item.setStack(remainder);
			}
			t.commit();
		}
	}

	@Override
	public boolean renderAsNormalBlockEntity() {
		return true;
	}

}

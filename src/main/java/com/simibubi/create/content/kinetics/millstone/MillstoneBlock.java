package com.simibubi.create.content.kinetics.millstone;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class MillstoneBlock extends KineticBlock implements IBE<MillstoneBlockEntity>, ICogWheel {

	public MillstoneBlock(Settings properties) {
		super(properties);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.MILLSTONE;
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.getStackInHand(handIn)
			.isEmpty())
			return ActionResult.PASS;
		if (worldIn.isClient)
			return ActionResult.SUCCESS;

		withBlockEntityDo(worldIn, pos, millstone -> {
			boolean emptyOutput = true;
			ItemStackHandler inv = millstone.outputInv;
			for (int slot = 0; slot < inv.getSlotCount(); slot++) {
				ItemStack stackInSlot = inv.getStackInSlot(slot);
				if (!stackInSlot.isEmpty())
					emptyOutput = false;
				player.getInventory()
					.offerOrDrop(stackInSlot);
				inv.setStackInSlot(slot, ItemStack.EMPTY);
			}

			if (emptyOutput) {
				inv = millstone.inputInv;
				for (int slot = 0; slot < inv.getSlotCount(); slot++) {
					player.getInventory()
						.offerOrDrop(inv.getStackInSlot(slot));
					inv.setStackInSlot(slot, ItemStack.EMPTY);
				}
			}

			millstone.markDirty();
			millstone.sendData();
		});

		return ActionResult.SUCCESS;
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);

		if (entityIn.getWorld().isClient)
			return;
		if (!(entityIn instanceof ItemEntity))
			return;
		if (!entityIn.isAlive())
			return;

		MillstoneBlockEntity millstone = null;
		for (BlockPos pos : Iterate.hereAndBelow(entityIn.getBlockPos()))
			if (millstone == null)
				millstone = getBlockEntity(worldIn, pos);

		if (millstone == null)
			return;

		ItemEntity itemEntity = (ItemEntity) entityIn;
		Storage<ItemVariant> handler = millstone.getItemStorage(null);
		if (handler == null)
			return;

		try (Transaction t = TransferUtil.getTransaction()) {
			ItemStack inEntity = itemEntity.getStack();
			long inserted = handler.insert(ItemVariant.of(inEntity), inEntity.getCount(), t);
			if (inserted == inEntity.getCount())
				itemEntity.discard();
			else itemEntity.setStack(ItemHandlerHelper.copyStackWithSize(inEntity, (int) (inEntity.getCount() - inserted)));
			t.commit();
		}
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	public Class<MillstoneBlockEntity> getBlockEntityClass() {
		return MillstoneBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MillstoneBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MILLSTONE.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}
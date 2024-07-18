package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.simibubi.create.foundation.utility.AdventureUtil;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SharedDepotBlockMethods {

	protected static DepotBehaviour get(BlockView worldIn, BlockPos pos) {
		return BlockEntityBehaviour.get(worldIn, pos, DepotBehaviour.TYPE);
	}

	public static ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
		Hand hand, BlockHitResult ray) {
		if (ray.getSide() != Direction.UP)
			return ActionResult.PASS;
		if (world.isClient)
			return ActionResult.SUCCESS;
		if (AdventureUtil.isAdventure(player))
			return ActionResult.PASS;

		DepotBehaviour behaviour = get(world, pos);
		if (behaviour == null)
			return ActionResult.PASS;
		if (!behaviour.canAcceptItems.get())
			return ActionResult.SUCCESS;

		ItemStack heldItem = player.getStackInHand(hand);
		boolean wasEmptyHanded = heldItem.isEmpty();
		boolean shouldntPlaceItem = AllBlocks.MECHANICAL_ARM.isIn(heldItem);

		ItemStack mainItemStack = behaviour.getHeldItemStack();
		if (!mainItemStack.isEmpty()) {
			player.getInventory()
				.offerOrDrop(mainItemStack);
			behaviour.removeHeldItem();
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f,
				1f + Create.RANDOM.nextFloat());
		}
		ItemStackHandler outputs = behaviour.processingOutputBuffer;
		try (Transaction t = TransferUtil.getTransaction()) {
			for (StorageView<ItemVariant> view : outputs.nonEmptyViews()) {
				ItemVariant var = view.getResource();
				long extracted = view.extract(var, 64, t);
				ItemStack stack = var.toStack(ItemHelper.truncateLong(extracted));
				player.getInventory().offerOrDrop(stack);
			}
			t.commit();
		}

		if (!wasEmptyHanded && !shouldntPlaceItem) {
			TransportedItemStack transported = new TransportedItemStack(heldItem);
			transported.insertedFrom = player.getHorizontalFacing();
			transported.prevBeltPosition = .25f;
			transported.beltPosition = .25f;
			behaviour.setHeldItem(transported);
			player.setStackInHand(hand, ItemStack.EMPTY);
			AllSoundEvents.DEPOT_SLIDE.playOnServer(world, pos);
		}

		behaviour.blockEntity.notifyUpdate();
		return ActionResult.SUCCESS;
	}

	public static void onLanded(BlockView worldIn, Entity entityIn) {
		if (!(entityIn instanceof ItemEntity))
			return;
		if (!entityIn.isAlive())
			return;
		if (entityIn.getWorld().isClient)
			return;

		ItemEntity itemEntity = (ItemEntity) entityIn;
		DirectBeltInputBehaviour inputBehaviour =
			BlockEntityBehaviour.get(worldIn, entityIn.getBlockPos(), DirectBeltInputBehaviour.TYPE);
		if (inputBehaviour == null)
			return;
		ItemStack remainder = inputBehaviour.handleInsertion(itemEntity.getStack(), Direction.DOWN, false);
		itemEntity.setStack(remainder);
		if (remainder.isEmpty())
			itemEntity.discard();
	}

	public static int getComparatorInputOverride(BlockState blockState, World worldIn, BlockPos pos) {
		DepotBehaviour depotBehaviour = get(worldIn, pos);
		if (depotBehaviour == null)
			return 0;
		float f = depotBehaviour.getPresentStackSize();
		Integer max = depotBehaviour.maxStackSize.get();
		f = f / (max == 0 ? 64 : max);
		return MathHelper.clamp(MathHelper.floor(f * 14.0F) + (f > 0 ? 1 : 0), 0, 15);
	}

}

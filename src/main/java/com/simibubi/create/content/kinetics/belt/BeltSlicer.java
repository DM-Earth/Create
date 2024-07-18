package com.simibubi.create.content.kinetics.belt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BeltSlicer {

	public static class Feedback {
		int color = 0xffffff;
		Box bb;
		String langKey;
		Formatting formatting = Formatting.WHITE;
	}

	public static ActionResult useWrench(BlockState state, World world, BlockPos pos, PlayerEntity player,
		Hand handIn, BlockHitResult hit, Feedback feedBack) {
		BeltBlockEntity controllerBE = BeltHelper.getControllerBE(world, pos);
		if (controllerBE == null)
			return ActionResult.PASS;
		if (state.get(BeltBlock.CASING) && hit.getSide() != Direction.UP)
			return ActionResult.PASS;
		if (state.get(BeltBlock.PART) == BeltPart.PULLEY && hit.getSide()
			.getAxis() != Axis.Y)
			return ActionResult.PASS;

		int beltLength = controllerBE.beltLength;
		if (beltLength == 2)
			return ActionResult.FAIL;

		BlockPos beltVector = BlockPos.ofFloored(BeltHelper.getBeltVector(state));
		BeltPart part = state.get(BeltBlock.PART);
		List<BlockPos> beltChain = BeltBlock.getBeltChain(world, controllerBE.getPos());
		boolean creative = player.isCreative();

		// Shorten from End
		if (hoveringEnd(state, hit)) {
			if (world.isClient)
				return ActionResult.SUCCESS;

			for (BlockPos blockPos : beltChain) {
				BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
				if (belt == null)
					continue;
				belt.detachKinetics();
				belt.invalidateItemHandler();
				belt.beltLength = 0;
			}

			BeltInventory inventory = controllerBE.inventory;
			BlockPos next = part == BeltPart.END ? pos.subtract(beltVector) : pos.add(beltVector);
			BlockState replacedState = world.getBlockState(next);
			BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, next);
			KineticBlockEntity.switchToBlockState(world, next, ProperWaterloggedBlock.withWater(world,
				state.with(BeltBlock.CASING, segmentBE != null && segmentBE.casing != CasingType.NONE), next));
			world.setBlockState(pos, ProperWaterloggedBlock.withWater(world, Blocks.AIR.getDefaultState(), pos),
				Block.NOTIFY_ALL | Block.MOVED);
			world.removeBlockEntity(pos);
			world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));

			if (!creative && AllBlocks.BELT.has(replacedState)
				&& replacedState.get(BeltBlock.PART) == BeltPart.PULLEY)
				player.getInventory().offerOrDrop(AllBlocks.SHAFT.asStack());

			// Eject overshooting items
			if (part == BeltPart.END && inventory != null) {
				List<TransportedItemStack> toEject = new ArrayList<>();
				for (TransportedItemStack transportedItemStack : inventory.getTransportedItems())
					if (transportedItemStack.beltPosition > beltLength - 1)
						toEject.add(transportedItemStack);
				toEject.forEach(inventory::eject);
				toEject.forEach(inventory.getTransportedItems()::remove);
			}

			// Transfer items to new controller
			if (part == BeltPart.START && segmentBE != null && inventory != null) {
				controllerBE.inventory = null;
				segmentBE.inventory = null;
				segmentBE.setController(next);
				for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
					transportedItemStack.beltPosition -= 1;
					if (transportedItemStack.beltPosition <= 0) {
						ItemEntity entity = new ItemEntity(world, pos.getX() + .5f, pos.getY() + 11 / 16f,
							pos.getZ() + .5f, transportedItemStack.stack);
						entity.setVelocity(Vec3d.ZERO);
						entity.setToDefaultPickupDelay();
						entity.velocityModified = true;
						world.spawnEntity(entity);
					} else
						segmentBE.getInventory()
							.addItem(transportedItemStack);
				}
			}

			return ActionResult.SUCCESS;
		}

		BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, pos);
		if (segmentBE == null)
			return ActionResult.PASS;

		// Split in half
		int hitSegment = segmentBE.index;
		Vec3d centerOf = VecHelper.getCenterOf(hit.getBlockPos());
		Vec3d subtract = hit.getPos()
			.subtract(centerOf);
		boolean towardPositive = subtract.dotProduct(Vec3d.of(beltVector)) > 0;
		BlockPos next = !towardPositive ? pos.subtract(beltVector) : pos.add(beltVector);

		if (hitSegment == 0 || hitSegment == 1 && !towardPositive)
			return ActionResult.FAIL;
		if (hitSegment == controllerBE.beltLength - 1 || hitSegment == controllerBE.beltLength - 2 && towardPositive)
			return ActionResult.FAIL;

		// Look for shafts
		if (!creative) {
			int requiredShafts = 0;
			if (!segmentBE.hasPulley())
				requiredShafts++;
			BlockState other = world.getBlockState(next);
			if (AllBlocks.BELT.has(other) && other.get(BeltBlock.PART) == BeltPart.MIDDLE)
				requiredShafts++;

			int amountRetrieved = 0;
			boolean beltFound = false;
			Search: while (true) {
				for (int i = 0; i < player.getInventory().size(); ++i) {
					if (amountRetrieved == requiredShafts && beltFound)
						break Search;

					ItemStack itemstack = player.getInventory().getStack(i);
					if (itemstack.isEmpty())
						continue;
					int count = itemstack.getCount();

					if (AllItems.BELT_CONNECTOR.isIn(itemstack)) {
						if (!world.isClient)
							itemstack.decrement(1);
						beltFound = true;
						continue;
					}

					if (AllBlocks.SHAFT.isIn(itemstack)) {
						int taken = Math.min(count, requiredShafts - amountRetrieved);
						if (!world.isClient)
							if (taken == count)
								player.getInventory().setStack(i, ItemStack.EMPTY);
							else
								itemstack.decrement(taken);
						amountRetrieved += taken;
					}
				}

				if (!world.isClient)
					player.getInventory().offerOrDrop(AllBlocks.SHAFT.asStack(amountRetrieved));
				return ActionResult.FAIL;
			}
		}

		if (!world.isClient) {
			for (BlockPos blockPos : beltChain) {
				BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
				if (belt == null)
					continue;
				belt.detachKinetics();
				belt.invalidateItemHandler();
				belt.beltLength = 0;
			}

			BeltInventory inventory = controllerBE.inventory;
			KineticBlockEntity.switchToBlockState(world, pos,
				state.with(BeltBlock.PART, towardPositive ? BeltPart.END : BeltPart.START));
			KineticBlockEntity.switchToBlockState(world, next, world.getBlockState(next)
				.with(BeltBlock.PART, towardPositive ? BeltPart.START : BeltPart.END));
			world.playSound(null, pos, SoundEvents.BLOCK_WOOL_HIT,
				player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 0.5F, 2.3F);

			// Transfer items to new controller
			BeltBlockEntity newController = towardPositive ? BeltHelper.getSegmentBE(world, next) : segmentBE;
			if (newController != null && inventory != null) {
				newController.inventory = null;
				newController.setController(newController.getPos());
				for (Iterator<TransportedItemStack> iterator = inventory.getTransportedItems()
					.iterator(); iterator.hasNext();) {
					TransportedItemStack transportedItemStack = iterator.next();
					float newPosition = transportedItemStack.beltPosition - hitSegment - (towardPositive ? 1 : 0);
					if (newPosition <= 0)
						continue;
					transportedItemStack.beltPosition = newPosition;
					iterator.remove();
					newController.getInventory()
						.addItem(transportedItemStack);
				}
			}
		}

		return ActionResult.SUCCESS;
	}

	public static ActionResult useConnector(BlockState state, World world, BlockPos pos, PlayerEntity player,
		Hand handIn, BlockHitResult hit, Feedback feedBack) {
		BeltBlockEntity controllerBE = BeltHelper.getControllerBE(world, pos);
		if (controllerBE == null)
			return ActionResult.PASS;

		int beltLength = controllerBE.beltLength;
		if (beltLength == BeltConnectorItem.maxLength())
			return ActionResult.FAIL;

		BlockPos beltVector = BlockPos.ofFloored(BeltHelper.getBeltVector(state));
		BeltPart part = state.get(BeltBlock.PART);
		Direction facing = state.get(BeltBlock.HORIZONTAL_FACING);
		List<BlockPos> beltChain = BeltBlock.getBeltChain(world, controllerBE.getPos());
		boolean creative = player.isCreative();

		if (!hoveringEnd(state, hit))
			return ActionResult.PASS;

		BlockPos next = part == BeltPart.START ? pos.subtract(beltVector) : pos.add(beltVector);
		BeltBlockEntity mergedController = null;
		int mergedBeltLength = 0;

		// Merge Belts / Extend at End
		BlockState nextState = world.getBlockState(next);
		if (!nextState.isReplaceable()) {
			if (!AllBlocks.BELT.has(nextState))
				return ActionResult.FAIL;
			if (!beltStatesCompatible(state, nextState))
				return ActionResult.FAIL;

			mergedController = BeltHelper.getControllerBE(world, next);
			if (mergedController == null)
				return ActionResult.FAIL;
			if (mergedController.beltLength + beltLength > BeltConnectorItem.maxLength())
				return ActionResult.FAIL;

			mergedBeltLength = mergedController.beltLength;

			if (!world.isClient) {
				boolean flipBelt = facing != nextState.get(BeltBlock.HORIZONTAL_FACING);
				Optional<DyeColor> color = controllerBE.color;
				for (BlockPos blockPos : BeltBlock.getBeltChain(world, mergedController.getPos())) {
					BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
					if (belt == null)
						continue;
					belt.detachKinetics();
					belt.invalidateItemHandler();
					belt.beltLength = 0;
					belt.color = color;
					if (flipBelt)
						world.setBlockState(blockPos, flipBelt(world.getBlockState(blockPos)), Block.NOTIFY_ALL | Block.MOVED);
				}

				// Reverse items
				if (flipBelt && mergedController.inventory != null) {
					List<TransportedItemStack> transportedItems = mergedController.inventory.getTransportedItems();
					for (TransportedItemStack transportedItemStack : transportedItems) {
						transportedItemStack.beltPosition = mergedBeltLength - transportedItemStack.beltPosition;
						transportedItemStack.prevBeltPosition =
							mergedBeltLength - transportedItemStack.prevBeltPosition;
					}
				}
			}
		}

		if (!world.isClient) {
			for (BlockPos blockPos : beltChain) {
				BeltBlockEntity belt = BeltHelper.getSegmentBE(world, blockPos);
				if (belt == null)
					continue;
				belt.detachKinetics();
				belt.invalidateItemHandler();
				belt.beltLength = 0;
			}

			BeltInventory inventory = controllerBE.inventory;
			KineticBlockEntity.switchToBlockState(world, pos, state.with(BeltBlock.PART, BeltPart.MIDDLE));

			if (mergedController == null) {
				// Attach at end
				world.setBlockState(next,
					ProperWaterloggedBlock.withWater(world, state.with(BeltBlock.CASING, false), next),
					Block.NOTIFY_ALL | Block.MOVED);
				BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, next);
				if (segmentBE != null)
					segmentBE.color = controllerBE.color;
				world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE,
					player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 0.5F, 1F);

				// Transfer items to new controller
				if (part == BeltPart.START && segmentBE != null && inventory != null) {
					segmentBE.setController(next);
					for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
						transportedItemStack.beltPosition += 1;
						segmentBE.getInventory()
							.addItem(transportedItemStack);
					}
				}

			} else {
				// Merge with other
				BeltInventory mergedInventory = mergedController.inventory;
				world.playSound(null, pos, SoundEvents.BLOCK_WOOL_HIT,
					player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 0.5F, 1.3F);
				BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(world, next);
				KineticBlockEntity.switchToBlockState(world, next,
					state.with(BeltBlock.CASING, segmentBE != null && segmentBE.casing != CasingType.NONE)
						.with(BeltBlock.PART, BeltPart.MIDDLE));

				if (!creative) {
					player.getInventory().offerOrDrop(AllBlocks.SHAFT.asStack(2));
					player.getInventory().offerOrDrop(AllItems.BELT_CONNECTOR.asStack());
				}

				// Transfer items to other controller
				BlockPos search = controllerBE.getPos();
				for (int i = 0; i < 10000; i++) {
					BlockState blockState = world.getBlockState(search);
					if (!AllBlocks.BELT.has(blockState))
						break;
					if (blockState.get(BeltBlock.PART) != BeltPart.START) {
						search = search.subtract(beltVector);
						continue;
					}

					BeltBlockEntity newController = BeltHelper.getSegmentBE(world, search);

					if (newController != controllerBE && inventory != null) {
						newController.setController(search);
						controllerBE.inventory = null;
						for (TransportedItemStack transportedItemStack : inventory.getTransportedItems()) {
							transportedItemStack.beltPosition += mergedBeltLength;
							newController.getInventory()
								.addItem(transportedItemStack);
						}
					}

					if (newController != mergedController && mergedInventory != null) {
						newController.setController(search);
						mergedController.inventory = null;
						for (TransportedItemStack transportedItemStack : mergedInventory.getTransportedItems()) {
							if (newController == controllerBE)
								transportedItemStack.beltPosition += beltLength;
							newController.getInventory()
								.addItem(transportedItemStack);
						}
					}

					break;
				}
			}
		}
		return ActionResult.SUCCESS;
	}

	static boolean beltStatesCompatible(BlockState state, BlockState nextState) {
		Direction facing1 = state.get(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope1 = state.get(BeltBlock.SLOPE);
		Direction facing2 = nextState.get(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope2 = nextState.get(BeltBlock.SLOPE);

		switch (slope1) {
		case UPWARD:
			if (slope2 == BeltSlope.DOWNWARD)
				return facing1 == facing2.getOpposite();
			return slope2 == slope1 && facing1 == facing2;
		case DOWNWARD:
			if (slope2 == BeltSlope.UPWARD)
				return facing1 == facing2.getOpposite();
			return slope2 == slope1 && facing1 == facing2;
		default:
			return slope2 == slope1 && facing2.getAxis() == facing1.getAxis();
		}
	}

	static BlockState flipBelt(BlockState state) {
		Direction facing = state.get(BeltBlock.HORIZONTAL_FACING);
		BeltSlope slope = state.get(BeltBlock.SLOPE);
		BeltPart part = state.get(BeltBlock.PART);

		if (slope == BeltSlope.UPWARD)
			state = state.with(BeltBlock.SLOPE, BeltSlope.DOWNWARD);
		else if (slope == BeltSlope.DOWNWARD)
			state = state.with(BeltBlock.SLOPE, BeltSlope.UPWARD);

		if (part == BeltPart.END)
			state = state.with(BeltBlock.PART, BeltPart.START);
		else if (part == BeltPart.START)
			state = state.with(BeltBlock.PART, BeltPart.END);

		return state.with(BeltBlock.HORIZONTAL_FACING, facing.getOpposite());
	}

	static boolean hoveringEnd(BlockState state, BlockHitResult hit) {
		BeltPart part = state.get(BeltBlock.PART);
		if (part == BeltPart.MIDDLE || part == BeltPart.PULLEY)
			return false;

		Vec3d beltVector = BeltHelper.getBeltVector(state);
		Vec3d centerOf = VecHelper.getCenterOf(hit.getBlockPos());
		Vec3d subtract = hit.getPos()
			.subtract(centerOf);

		return subtract.dotProduct(beltVector) > 0 == (part == BeltPart.END);
	}

	@Environment(EnvType.CLIENT)
	public static void tickHoveringInformation() {
		MinecraftClient mc = MinecraftClient.getInstance();
		HitResult target = mc.crosshairTarget;
		if (target == null || !(target instanceof BlockHitResult))
			return;

		BlockHitResult result = (BlockHitResult) target;
		ClientWorld world = mc.world;
		BlockPos pos = result.getBlockPos();
		BlockState state = world.getBlockState(pos);
		ItemStack held = mc.player.getStackInHand(Hand.MAIN_HAND);
		ItemStack heldOffHand = mc.player.getStackInHand(Hand.OFF_HAND);

		if (mc.player.isSneaking())
			return;
		if (!AllBlocks.BELT.has(state))
			return;

		Feedback feedback = new Feedback();

		// TODO: Populate feedback in the methods for clientside
		if (AllItems.WRENCH.isIn(held) || AllItems.WRENCH.isIn(heldOffHand))
			useWrench(state, world, pos, mc.player, Hand.MAIN_HAND, result, feedback);
		else if (AllItems.BELT_CONNECTOR.isIn(held) || AllItems.BELT_CONNECTOR.isIn(heldOffHand))
			useConnector(state, world, pos, mc.player, Hand.MAIN_HAND, result, feedback);
		else
			return;

		if (feedback.langKey != null)
			mc.player.sendMessage(Lang.translateDirect(feedback.langKey)
				.formatted(feedback.formatting), true);
		else
			mc.player.sendMessage(Components.immutableEmpty(), true);

		if (feedback.bb != null)
			CreateClient.OUTLINER.chaseAABB("BeltSlicer", feedback.bb)
				.lineWidth(1 / 16f)
				.colored(feedback.color);
	}

}

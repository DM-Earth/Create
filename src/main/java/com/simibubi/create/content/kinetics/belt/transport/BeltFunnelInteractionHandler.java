package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock.Shape;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BeltFunnelInteractionHandler {

	public static boolean checkForFunnels(BeltInventory beltInventory, TransportedItemStack currentItem,
		float nextOffset) {
		boolean beltMovementPositive = beltInventory.beltMovementPositive;
		int firstUpcomingSegment = (int) Math.floor(currentItem.beltPosition);
		int step = beltMovementPositive ? 1 : -1;
		firstUpcomingSegment = MathHelper.clamp(firstUpcomingSegment, 0, beltInventory.belt.beltLength - 1);

		for (int segment = firstUpcomingSegment; beltMovementPositive ? segment <= nextOffset
			: segment + 1 >= nextOffset; segment += step) {
			BlockPos funnelPos = BeltHelper.getPositionForOffset(beltInventory.belt, segment)
				.up();
			World world = beltInventory.belt.getWorld();
			BlockState funnelState = world.getBlockState(funnelPos);
			if (!(funnelState.getBlock() instanceof BeltFunnelBlock))
				continue;
			Direction funnelFacing = funnelState.get(BeltFunnelBlock.HORIZONTAL_FACING);
			Direction movementFacing = beltInventory.belt.getMovementFacing();
			boolean blocking = funnelFacing == movementFacing.getOpposite();
			if (funnelFacing == movementFacing)
				continue;
			if (funnelState.get(BeltFunnelBlock.SHAPE) == Shape.PUSHING)
				continue;

			float funnelEntry = segment + .5f;
			if (funnelState.get(BeltFunnelBlock.SHAPE) == Shape.EXTENDED)
				funnelEntry += .499f * (beltMovementPositive ? -1 : 1);

			boolean hasCrossed = nextOffset > funnelEntry && beltMovementPositive
				|| nextOffset < funnelEntry && !beltMovementPositive;
			if (!hasCrossed)
				return false;
			if (blocking)
				currentItem.beltPosition = funnelEntry;

			if (world.isClient || funnelState.getOrEmpty(BeltFunnelBlock.POWERED).orElse(false))
				if (blocking)
					return true;
				else
					continue;

			BlockEntity be = world.getBlockEntity(funnelPos);
			if (!(be instanceof FunnelBlockEntity))
				return true;

			FunnelBlockEntity funnelBE = (FunnelBlockEntity) be;
			InvManipulationBehaviour inserting = funnelBE.getBehaviour(InvManipulationBehaviour.TYPE);
			FilteringBehaviour filtering = funnelBE.getBehaviour(FilteringBehaviour.TYPE);

			if (inserting == null || filtering != null && !filtering.test(currentItem.stack))
				if (blocking)
					return true;
				else
					continue;

			int amountToExtract = funnelBE.getAmountToExtract();
			ExtractionCountMode modeToExtract = funnelBE.getModeToExtract();

			ItemStack toInsert = currentItem.stack.copy();
			if (amountToExtract > toInsert.getCount() && modeToExtract != ExtractionCountMode.UPTO)
				if (blocking)
					return true;
				else
					continue;

			if (amountToExtract != -1 && modeToExtract != ExtractionCountMode.UPTO) {
				toInsert.setCount(Math.min(amountToExtract, toInsert.getCount()));
				ItemStack remainder = inserting.simulate()
					.insert(toInsert);
				if (!remainder.isEmpty())
					if (blocking)
						return true;
					else
						continue;
			}

			ItemStack remainder = inserting.insert(toInsert);
			if (ItemStack.areEqual(remainder, toInsert))
				if (blocking)
					return true;
				else
					continue;

			int notFilled = currentItem.stack.getCount() - toInsert.getCount();
			if (!remainder.isEmpty()) {
				remainder.increment(notFilled);
			} else if (notFilled > 0)
				remainder = ItemHandlerHelper.copyStackWithSize(currentItem.stack, notFilled);

			funnelBE.flap(true);
			funnelBE.onTransfer(toInsert);
			currentItem.stack = remainder;
			beltInventory.belt.sendData();
			// fabric: fully inserted, early exit to avoid inserting an empty stack on next loop
			if (remainder.isEmpty()) {
				return false;
			}
			if (blocking)
				return true;
		}

		return false;
	}

}
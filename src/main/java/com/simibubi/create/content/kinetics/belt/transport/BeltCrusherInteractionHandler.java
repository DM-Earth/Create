package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.util.ItemStackUtil;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BeltCrusherInteractionHandler {

    public static boolean checkForCrushers(BeltInventory beltInventory, TransportedItemStack currentItem,
                                           float nextOffset) {

        boolean beltMovementPositive = beltInventory.beltMovementPositive;
        int firstUpcomingSegment = (int) Math.floor(currentItem.beltPosition);
        int step = beltMovementPositive ? 1 : -1;
        firstUpcomingSegment = MathHelper.clamp(firstUpcomingSegment, 0, beltInventory.belt.beltLength - 1);

        for (int segment = firstUpcomingSegment; beltMovementPositive ? segment <= nextOffset
                : segment + 1 >= nextOffset; segment += step) {
            BlockPos crusherPos = BeltHelper.getPositionForOffset(beltInventory.belt, segment)
                    .up();
            World world = beltInventory.belt.getWorld();
            BlockState crusherState = world.getBlockState(crusherPos);
            if (!(crusherState.getBlock() instanceof CrushingWheelControllerBlock))
                continue;
            Direction crusherFacing = crusherState.get(CrushingWheelControllerBlock.FACING);
            Direction movementFacing = beltInventory.belt.getMovementFacing();
            if (crusherFacing != movementFacing)
                continue;

            float crusherEntry = segment + .5f;
            crusherEntry += .399f * (beltMovementPositive ? -1 : 1);
            float postCrusherEntry = crusherEntry + .799f * (!beltMovementPositive ? -1 : 1);

            boolean hasCrossed = nextOffset > crusherEntry && nextOffset < postCrusherEntry && beltMovementPositive
                    || nextOffset < crusherEntry && nextOffset > postCrusherEntry && !beltMovementPositive;
            if (!hasCrossed)
                return false;
            currentItem.beltPosition = crusherEntry;

            BlockEntity be = world.getBlockEntity(crusherPos);
            if (!(be instanceof CrushingWheelControllerBlockEntity))
                return true;

            CrushingWheelControllerBlockEntity crusherBE = (CrushingWheelControllerBlockEntity) be;

            ItemStack toInsert = currentItem.stack.copy();
			try (Transaction t = TransferUtil.getTransaction()) {
				long inserted = crusherBE.inventory.insert(ItemVariant.of(toInsert), toInsert.getCount(), t);
				t.commit();
				ItemStack remainder = ItemHandlerHelper.copyStackWithSize(toInsert, toInsert.getCount() - (int) inserted);
				if (ItemStack.areEqual(toInsert, remainder))
					return true;

				int notFilled = currentItem.stack.getCount() - toInsert.getCount();
				if (!remainder.isEmpty()) {
					remainder.increment(notFilled);
				} else if (notFilled > 0)
					remainder = ItemHandlerHelper.copyStackWithSize(currentItem.stack, notFilled);

				currentItem.stack = remainder;
				beltInventory.belt.sendData();
				return true;
			}
        }

        return false;
    }


}

package com.simibubi.create.content.contraptions.behaviour.dispenser;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class DropperMovementBehaviour implements MovementBehaviour {
	protected static final MovedDefaultDispenseItemBehaviour DEFAULT_BEHAVIOUR =
		new MovedDefaultDispenseItemBehaviour();
	private static final Random RNG = Random.create();

	protected void activate(MovementContext context, BlockPos pos) {
		DispenseItemLocation location = getDispenseLocation(context);
		if (location.isEmpty()) {
			context.world.syncWorldEvent(1001, pos, 0);
		} else {
			setItemStackAt(location, DEFAULT_BEHAVIOUR.dispense(getItemStackAt(location, context), context, pos),
				context);
		}
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (context.world.isClient)
			return;
		collectItems(context);
		activate(context, pos);
	}

	private void collectItems(MovementContext context) {
		getStacks(context).stream()
			.filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() != Items.AIR
				&& itemStack.getMaxCount() > itemStack.getCount())
			.forEach(itemStack -> itemStack.increment(ItemHelper
				.extract(context.contraption.getSharedInventory(), ItemHelper.sameItemPredicate(itemStack),
					ItemHelper.ExtractionCountMode.UPTO, itemStack.getMaxCount() - itemStack.getCount(), false)
				.getCount()));
	}

	private void updateTemporaryData(MovementContext context) {
		if (!(context.temporaryData instanceof DefaultedList) && context.world != null) {
			DefaultedList<ItemStack> stacks = DefaultedList.ofSize(getInvSize(), ItemStack.EMPTY);
			Inventories.readNbt(context.blockEntityData, stacks);
			context.temporaryData = stacks;
		}
	}

	@SuppressWarnings("unchecked")
	private DefaultedList<ItemStack> getStacks(MovementContext context) {
		updateTemporaryData(context);
		return (DefaultedList<ItemStack>) context.temporaryData;
	}

	private ArrayList<DispenseItemLocation> getUseableLocations(MovementContext context) {
		ArrayList<DispenseItemLocation> useable = new ArrayList<>();
		try (Transaction t = TransferUtil.getTransaction()) {
			for (int slot = 0; slot < getInvSize(); slot++) {
				DispenseItemLocation location = new DispenseItemLocation(slot);
				ItemStack testStack = getItemStackAt(location, context);
				if (testStack == null || testStack.isEmpty())
					continue;
				if (testStack.getMaxCount() == 1) {
					ResourceAmount<ItemVariant> available = StorageUtil.findExtractableContent(context.contraption.getSharedInventory(), v -> v.matches(testStack), t);
					if (available != null) {
						location = new DispenseItemLocation(available);
						useable.add(location);
					}
				} else if (testStack.getCount() >= 2)
					useable.add(location);
			}
			return useable;
		}
	}

	@Override
	public void writeExtraData(MovementContext context) {
		DefaultedList<ItemStack> stacks = getStacks(context);
		if (stacks == null)
			return;
		Inventories.writeNbt(context.blockEntityData, stacks);
	}

	@Override
	public void stopMoving(MovementContext context) {
		MovementBehaviour.super.stopMoving(context);
		writeExtraData(context);
	}

	protected DispenseItemLocation getDispenseLocation(MovementContext context) {
		int i = -1;
		int j = 1;
		List<DispenseItemLocation> useableLocations = getUseableLocations(context);
		for (int k = 0; k < useableLocations.size(); ++k) {
			if (RNG.nextInt(j++) == 0) {
				i = k;
			}
		}
		if (i < 0)
			return DispenseItemLocation.NONE;
		else
			return useableLocations.get(i);
	}

	protected ItemStack getItemStackAt(DispenseItemLocation location, MovementContext context) {
		if (location.isInternal()) {
			return getStacks(context).get(location.getSlot());
		} else {
			return location.getVariant().toStack(location.getCount());
		}
	}

	protected void setItemStackAt(DispenseItemLocation location, ItemStack stack, MovementContext context) {
		if (location.isInternal()) {
			getStacks(context).set(location.getSlot(), stack);
		} else {
			try (Transaction t = TransferUtil.getTransaction()) {
				context.contraption.getSharedInventory()
				.extract(location.getVariant(), location.getCount(), t);
				context.contraption.getSharedInventory().insert(ItemVariant.of(stack), stack.getCount(), t);
				t.commit();
			}
		}
	}

	private static int getInvSize() {
		return 9;
	}
}

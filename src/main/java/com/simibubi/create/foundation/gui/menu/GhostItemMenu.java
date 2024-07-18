package com.simibubi.create.foundation.gui.menu;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public abstract class GhostItemMenu<T> extends MenuBase<T> implements IClearableMenu {

	public ItemStackHandler ghostInventory;

	protected GhostItemMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected GhostItemMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, T contentHolder) {
		super(type, id, inv, contentHolder);
	}

	protected abstract ItemStackHandler createGhostInventory();

	protected abstract boolean allowRepeats();

	@Override
	protected void initAndReadInventory(T contentHolder) {
		ghostInventory = createGhostInventory();
	}

	@Override
	public void clearContents() {
		for (int i = 0; i < ghostInventory.getSlotCount(); i++)
			ghostInventory.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slotIn) {
		return slotIn.inventory == playerInventory;
	}

	@Override
	public boolean canInsertIntoSlot(Slot slotIn) {
		if (allowRepeats())
			return true;
		return slotIn.inventory == playerInventory;
	}

	@Override
	public void onSlotClick(int slotId, int dragType, SlotActionType clickTypeIn, PlayerEntity player) {
		if (slotId < 36) {
			super.onSlotClick(slotId, dragType, clickTypeIn, player);
			return;
		}
		if (clickTypeIn == SlotActionType.THROW)
			return;

		ItemStack held = getCursorStack();
		int slot = slotId - 36;
		if (clickTypeIn == SlotActionType.CLONE) {
			if (player.isCreative() && held.isEmpty()) {
				ItemStack stackInSlot = ghostInventory.getStackInSlot(slot)
						.copy();
				stackInSlot.setCount(stackInSlot.getMaxCount());
				setCursorStack(stackInSlot);
				return;
			}
			return;
		}

		ItemStack insert;
		if (held.isEmpty()) {
			insert = ItemStack.EMPTY;
		} else {
			insert = held.copy();
			insert.setCount(1);
		}
		ghostInventory.setStackInSlot(slot, insert);
		getSlot(slotId).markDirty();
	}

	@Override
	public ItemStack quickMove(PlayerEntity playerIn, int index) {
		if (index < 36) {
			ItemStack stackToInsert = playerInventory.getStack(index);
			for (int i = 0; i < ghostInventory.getSlotCount(); i++) {
				ItemStack stack = ghostInventory.getStackInSlot(i);
				if (!allowRepeats() && ItemHandlerHelper.canItemStacksStack(stack, stackToInsert))
					break;
				if (stack.isEmpty()) {
					ItemStack copy = stackToInsert.copy();
					copy.setCount(1);
					ghostInventory.setStackInSlot(i, copy);
					getSlot(i + 36).markDirty();
					break;
				}
			}
		} else {
			ghostInventory.setStackInSlot(index - 36, ItemStack.EMPTY);
			getSlot(index).markDirty();
		}
		return ItemStack.EMPTY;
	}

}

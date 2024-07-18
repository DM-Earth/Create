package com.simibubi.create.content.trains.schedule;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ScheduleMenu extends GhostItemMenu<ItemStack> {

	public boolean slotsActive = true;
	public int targetSlotsActive = 1;

	static final int slots = 2;
	public ScheduleMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public ScheduleMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ItemStack contentHolder) {
		super(type, id, inv, contentHolder);
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return new ItemStackHandler(slots);
	}

	@Override
	public void onSlotClick(int slotId, int dragType, SlotActionType clickTypeIn, PlayerEntity player) {
		if (slotId != playerInventory.selectedSlot || clickTypeIn == SlotActionType.THROW)
			super.onSlotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	protected boolean allowRepeats() {
		return true;
	}

	@Override
	protected ItemStack createOnClient(PacketByteBuf extraData) {
		return extraData.readItemStack();
	}

	@Override
	protected void addSlots() {
		addPlayerSlots(46, 140);
		for (int i = 0; i < slots; i++)
			addSlot(new InactiveItemHandlerSlot(ghostInventory, i, i, 54 + 20 * i, 88));
	}

	@Override
	protected void addPlayerSlots(int x, int y) {
		for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot)
			this.addSlot(new InactiveSlot(playerInventory, hotbarSlot, x + hotbarSlot * 18, y + 58));
		for (int row = 0; row < 3; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new InactiveSlot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
	}

	@Override
	protected void saveData(ItemStack contentHolder) {}

	@Override
	public boolean canUse(PlayerEntity player) {
		return playerInventory.getMainHandStack() == contentHolder;
	}

	class InactiveSlot extends Slot {

		public InactiveSlot(Inventory pContainer, int pIndex, int pX, int pY) {
			super(pContainer, pIndex, pX, pY);
		}

		@Override
		public boolean isEnabled() {
			return slotsActive;
		}

	}

	class InactiveItemHandlerSlot extends SlotItemHandler {
private int targetIndex;

		public InactiveItemHandlerSlot(ItemStackHandler itemHandler, int targetIndex, int index, int xPosition,
			int yPosition) {
			super(itemHandler, index, xPosition, yPosition);
			this.targetIndex = targetIndex;
		}

		@Override
		public boolean isEnabled() {
			return slotsActive && targetIndex < targetSlotsActive;
		}

	}

}

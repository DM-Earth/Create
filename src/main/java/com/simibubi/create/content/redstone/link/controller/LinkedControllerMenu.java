package com.simibubi.create.content.redstone.link.controller;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

public class LinkedControllerMenu extends GhostItemMenu<ItemStack> {

	public LinkedControllerMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public LinkedControllerMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ItemStack filterItem) {
		super(type, id, inv, filterItem);
	}

	public static LinkedControllerMenu create(int id, PlayerInventory inv, ItemStack filterItem) {
		return new LinkedControllerMenu(AllMenuTypes.LINKED_CONTROLLER.get(), id, inv, filterItem);
	}

	@Override
	protected ItemStack createOnClient(PacketByteBuf extraData) {
		return extraData.readItemStack();
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return LinkedControllerItem.getFrequencyItems(contentHolder);
	}

	@Override
	protected void addSlots() {
		addPlayerSlots(8, 131);

		int x = 12;
		int y = 34;
		int slot = 0;

		for (int column = 0; column < 6; column++) {
			for (int row = 0; row < 2; ++row)
				addSlot(new SlotItemHandler(ghostInventory, slot++, x, y + row * 18));
			x += 24;
			if (column == 3)
				x += 11;
		}
	}

	@Override
	protected void saveData(ItemStack contentHolder) {
		contentHolder.getOrCreateNbt()
			.put("Items", ghostInventory.serializeNBT());
	}

	@Override
	protected boolean allowRepeats() {
		return true;
	}

	@Override
	public void onSlotClick(int slotId, int dragType, SlotActionType clickTypeIn, PlayerEntity player) {
		if (slotId == playerInventory.selectedSlot && clickTypeIn != SlotActionType.THROW)
			return;
		super.onSlotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean canUse(PlayerEntity playerIn) {
		return playerInventory.getMainHandStack() == contentHolder;
	}

}

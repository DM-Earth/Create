package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.foundation.gui.menu.GhostItemMenu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

public abstract class AbstractFilterMenu extends GhostItemMenu<ItemStack> {

	protected AbstractFilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected AbstractFilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ItemStack contentHolder) {
		super(type, id, inv, contentHolder);
	}

	@Override
	public void onSlotClick(int slotId, int dragType, SlotActionType clickTypeIn, PlayerEntity player) {
		if (slotId == playerInventory.selectedSlot && clickTypeIn != SlotActionType.THROW)
			return;
		super.onSlotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	protected boolean allowRepeats() {
		return false;
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected ItemStack createOnClient(PacketByteBuf extraData) {
		return extraData.readItemStack();
	}

	protected abstract int getPlayerInventoryXOffset();

	protected abstract int getPlayerInventoryYOffset();

	protected abstract void addFilterSlots();

	@Override
	protected void addSlots() {
		addPlayerSlots(getPlayerInventoryXOffset(), getPlayerInventoryYOffset());
		addFilterSlots();
	}

	@Override
	protected void saveData(ItemStack contentHolder) {
		contentHolder.getOrCreateNbt()
				.put("Items", ghostInventory.serializeNBT());
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return playerInventory.getMainHandStack() == contentHolder;
	}

}

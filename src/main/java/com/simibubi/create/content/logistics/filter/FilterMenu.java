package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllMenuTypes;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;

public class FilterMenu extends AbstractFilterMenu {

	boolean respectNBT;
	boolean blacklist;

	public FilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public FilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ItemStack stack) {
		super(type, id, inv, stack);
	}

	public static FilterMenu create(int id, PlayerInventory inv, ItemStack stack) {
		return new FilterMenu(AllMenuTypes.FILTER.get(), id, inv, stack);
	}

	@Override
	protected int getPlayerInventoryXOffset() {
		return 38;
	}

	@Override
	protected int getPlayerInventoryYOffset() {
		return 121;
	}

	@Override
	protected void addFilterSlots() {
		int x = 23;
		int y = 22;
		for (int row = 0; row < 2; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new SlotItemHandler(ghostInventory, col + row * 9, x + col * 18, y + row * 18));
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return FilterItem.getFilterItems(contentHolder);
	}

	@Override
	protected void initAndReadInventory(ItemStack filterItem) {
		super.initAndReadInventory(filterItem);
		NbtCompound tag = filterItem.getOrCreateNbt();
		respectNBT = tag.getBoolean("RespectNBT");
		blacklist = tag.getBoolean("Blacklist");
	}

	@Override
	protected void saveData(ItemStack filterItem) {
		super.saveData(filterItem);
		NbtCompound tag = filterItem.getOrCreateNbt();
		tag.putBoolean("RespectNBT", respectNBT);
		tag.putBoolean("Blacklist", blacklist);

		if (respectNBT || blacklist)
			return;
		for (int i = 0; i < ghostInventory.getSlotCount(); i++)
			if (!ghostInventory.getStackInSlot(i)
				.isEmpty())
				return;
		filterItem.setNbt(null);
	}

}

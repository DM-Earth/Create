package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.foundation.utility.IInteractionChecker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public abstract class MenuBase<T> extends ScreenHandler {

	public PlayerEntity player;
	public PlayerInventory playerInventory;
	public T contentHolder;

	protected MenuBase(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id);
		init(inv, createOnClient(extraData));
	}

	protected MenuBase(ScreenHandlerType<?> type, int id, PlayerInventory inv, T contentHolder) {
		super(type, id);
		init(inv, contentHolder);
	}

	protected void init(PlayerInventory inv, T contentHolderIn) {
		player = inv.player;
		playerInventory = inv;
		contentHolder = contentHolderIn;
		initAndReadInventory(contentHolder);
		addSlots();
		sendContentUpdates();
	}

	@Environment(EnvType.CLIENT)
	protected abstract T createOnClient(PacketByteBuf extraData);

	protected abstract void initAndReadInventory(T contentHolder);

	protected abstract void addSlots();

	protected abstract void saveData(T contentHolder);

	protected void addPlayerSlots(int x, int y) {
		for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot)
			this.addSlot(new Slot(playerInventory, hotbarSlot, x + hotbarSlot * 18, y + 58));
		for (int row = 0; row < 3; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
	}

	@Override
	public void onClosed(PlayerEntity playerIn) {
		super.onClosed(playerIn);
		saveData(contentHolder);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		if (contentHolder == null)
			return false;
		if (contentHolder instanceof IInteractionChecker)
			return ((IInteractionChecker) contentHolder).canPlayerUse(player);
		return true;
	}

}

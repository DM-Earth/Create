package com.simibubi.create.content.schematics.cannon;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class SchematicannonMenu extends MenuBase<SchematicannonBlockEntity> {

	public SchematicannonMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf buffer) {
		super(type, id, inv, buffer);
	}

	public SchematicannonMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, SchematicannonBlockEntity be) {
		super(type, id, inv, be);
	}

	public static SchematicannonMenu create(int id, PlayerInventory inv, SchematicannonBlockEntity be) {
		return new SchematicannonMenu(AllMenuTypes.SCHEMATICANNON.get(), id, inv, be);
	}

	@Override
	protected SchematicannonBlockEntity createOnClient(PacketByteBuf extraData) {
		ClientWorld world = MinecraftClient.getInstance().world;
		BlockEntity blockEntity = world.getBlockEntity(extraData.readBlockPos());
		if (blockEntity instanceof SchematicannonBlockEntity schematicannon) {
			schematicannon.readClient(extraData.readNbt());
			return schematicannon;
		}
		return null;
	}

	@Override
	protected void initAndReadInventory(SchematicannonBlockEntity contentHolder) {
	}

	@Override
	protected void addSlots() {
		int x = 0;
		int y = 0;

		addSlot(new SlotItemHandler(contentHolder.inventory, 0, x + 15, y + 65));
		addSlot(new SlotItemHandler(contentHolder.inventory, 1, x + 171, y + 65));
		addSlot(new SlotItemHandler(contentHolder.inventory, 2, x + 134, y + 19));
		addSlot(new SlotItemHandler(contentHolder.inventory, 3, x + 174, y + 19));
		addSlot(new SlotItemHandler(contentHolder.inventory, 4, x + 15, y + 19));

		addPlayerSlots(37, 161);
	}

	@Override
	protected void saveData(SchematicannonBlockEntity contentHolder) {
	}

	@Override
	public ItemStack quickMove(PlayerEntity playerIn, int index) {
		Slot clickedSlot = getSlot(index);
		if (!clickedSlot.hasStack())
			return ItemStack.EMPTY;
		ItemStack stack = clickedSlot.getStack();

		if (index < 5) {
			insertItem(stack, 5, slots.size(), false);
		} else {
			if (insertItem(stack, 0, 1, false) || insertItem(stack, 2, 3, false)
					|| insertItem(stack, 4, 5, false))
				;
		}

		return ItemStack.EMPTY;
	}

}

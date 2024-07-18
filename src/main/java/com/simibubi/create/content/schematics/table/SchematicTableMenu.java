package com.simibubi.create.content.schematics.table;

import com.simibubi.create.AllItems;
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

public class SchematicTableMenu extends MenuBase<SchematicTableBlockEntity> {

	private Slot inputSlot;
	private Slot outputSlot;

	public SchematicTableMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public SchematicTableMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, SchematicTableBlockEntity be) {
		super(type, id, inv, be);
	}

	public static SchematicTableMenu create(int id, PlayerInventory inv, SchematicTableBlockEntity be) {
		return new SchematicTableMenu(AllMenuTypes.SCHEMATIC_TABLE.get(), id, inv, be);
	}

	public boolean canWrite() {
		return inputSlot.hasStack() && !outputSlot.hasStack();
	}

	@Override
	public ItemStack quickMove(PlayerEntity playerIn, int index) {
		Slot clickedSlot = getSlot(index);
		if (!clickedSlot.hasStack())
			return ItemStack.EMPTY;

		ItemStack stack = clickedSlot.getStack();
		if (index < 2)
			insertItem(stack, 2, slots.size(), false);
		else
			insertItem(stack, 0, 1, false);

		return ItemStack.EMPTY;
	}

	@Override
	protected SchematicTableBlockEntity createOnClient(PacketByteBuf extraData) {
		ClientWorld world = MinecraftClient.getInstance().world;
		BlockEntity blockEntity = world.getBlockEntity(extraData.readBlockPos());
		if (blockEntity instanceof SchematicTableBlockEntity schematicTable) {
			schematicTable.readClient(extraData.readNbt());
			return schematicTable;
		}
		return null;
	}

	@Override
	protected void initAndReadInventory(SchematicTableBlockEntity contentHolder) {
	}

	@Override
	protected void addSlots() {
		inputSlot = new SlotItemHandler(contentHolder.inventory, 0, 21, 57) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return AllItems.EMPTY_SCHEMATIC.isIn(stack) || AllItems.SCHEMATIC_AND_QUILL.isIn(stack)
						|| AllItems.SCHEMATIC.isIn(stack);
			}
		};

		outputSlot = new SlotItemHandler(contentHolder.inventory, 1, 166, 57) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}
		};

		addSlot(inputSlot);
		addSlot(outputSlot);

		// player Slots
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 38 + col * 18, 105 + row * 18));
			}
		}

		for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
			this.addSlot(new Slot(player.getInventory(), hotbarSlot, 38 + hotbarSlot * 18, 163));
		}
	}

	@Override
	protected void saveData(SchematicTableBlockEntity contentHolder) {
	}

}

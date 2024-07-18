package com.simibubi.create.content.equipment.toolbox;

import static com.simibubi.create.content.equipment.toolbox.ToolboxInventory.STACKS_PER_COMPARTMENT;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.gui.menu.MenuBase;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class ToolboxMenu extends MenuBase<ToolboxBlockEntity> {

	public ToolboxMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public ToolboxMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ToolboxBlockEntity be) {
		super(type, id, inv, be);
		be.startOpen(player);
	}

	public static ToolboxMenu create(int id, PlayerInventory inv, ToolboxBlockEntity be) {
		return new ToolboxMenu(AllMenuTypes.TOOLBOX.get(), id, inv, be);
	}

	@Override
	protected ToolboxBlockEntity createOnClient(PacketByteBuf extraData) {
		BlockPos readBlockPos = extraData.readBlockPos();
		NbtCompound readNbt = extraData.readNbt();

		ClientWorld world = MinecraftClient.getInstance().world;
		BlockEntity blockEntity = world.getBlockEntity(readBlockPos);
		if (blockEntity instanceof ToolboxBlockEntity) {
			ToolboxBlockEntity toolbox = (ToolboxBlockEntity) blockEntity;
			toolbox.readClient(readNbt);
			return toolbox;
		}

		return null;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		Slot clickedSlot = getSlot(index);
		if (!clickedSlot.hasStack())
			return ItemStack.EMPTY;

		ItemStack stack = clickedSlot.getStack();
		int size = contentHolder.inventory.getSlotCount();
		boolean success = false;
		if (index < size) {
			success = !insertItem(stack, size, slots.size(), false);
			contentHolder.inventory.onContentsChanged(index);
		} else
			success = !insertItem(stack, 0, size - 1, false);

		return success ? ItemStack.EMPTY : stack;
	}

	@Override
	protected void initAndReadInventory(ToolboxBlockEntity contentHolder) {

	}

	@Override
	public void onSlotClick(int index, int flags, SlotActionType type, PlayerEntity player) {
		int size = contentHolder.inventory.getSlotCount();

		if (index >= 0 && index < size) {
			ItemStack itemInClickedSlot = getSlot(index).getStack();
			ItemStack carried = getCursorStack();

			if (type == SlotActionType.PICKUP && !carried.isEmpty() && !itemInClickedSlot.isEmpty()
				&& ToolboxInventory.canItemsShareCompartment(itemInClickedSlot, carried)) {
				int subIndex = index % STACKS_PER_COMPARTMENT;
				if (subIndex != STACKS_PER_COMPARTMENT - 1) {
					onSlotClick(index - subIndex + STACKS_PER_COMPARTMENT - 1, flags, type, player);
					return;
				}
			}

			if (type == SlotActionType.PICKUP && carried.isEmpty() && itemInClickedSlot.isEmpty())
				if (!player.getWorld().isClient) {
					contentHolder.inventory.filters.set(index / STACKS_PER_COMPARTMENT, ItemStack.EMPTY);
					contentHolder.sendData();
				}

		}
		super.onSlotClick(index, flags, type, player);
	}

	@Override
	public boolean canInsertIntoSlot(Slot slot) {
		return slot.id > contentHolder.inventory.getSlotCount() && super.canInsertIntoSlot(slot);
	}

	public ItemStack getFilter(int compartment) {
		return contentHolder.inventory.filters.get(compartment);
	}

	public int totalCountInCompartment(int compartment) {
		int count = 0;
		int baseSlot = compartment * STACKS_PER_COMPARTMENT;
		for (int i = 0; i < STACKS_PER_COMPARTMENT; i++)
			count += getSlot(baseSlot + i).getStack()
				.getCount();
		return count;
	}

	public boolean renderPass;

	@Override
	protected void addSlots() {
		ToolboxInventory inventory = contentHolder.inventory;

		int x = 79;
		int y = 37;

		int[] xOffsets = { x, x + 33, x + 66, x + 66 + 6, x + 66, x + 33, x, x - 6 };
		int[] yOffsets = { y, y - 6, y, y + 33, y + 66, y + 66 + 6, y + 66, y + 33 };

		for (int compartment = 0; compartment < 8; compartment++) {
			int baseIndex = compartment * STACKS_PER_COMPARTMENT;

			// Representative Slots
			addSlot(new ToolboxSlot(this, inventory, baseIndex, xOffsets[compartment], yOffsets[compartment]));

			// Hidden Slots
			for (int i = 1; i < STACKS_PER_COMPARTMENT; i++)
				addSlot(new SlotItemHandler(inventory, baseIndex + i, -10000, -10000));
		}

		addPlayerSlots(8, 165);
	}

	@Override
	protected void saveData(ToolboxBlockEntity contentHolder) {

	}

	@Override
	public void onClosed(PlayerEntity playerIn) {
		super.onClosed(playerIn);
		if (!playerIn.getWorld().isClient)
			contentHolder.stopOpen(playerIn);
	}

}

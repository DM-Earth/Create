package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Pair;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;

public class AttributeFilterMenu extends AbstractFilterMenu {

	public enum WhitelistMode {
		WHITELIST_DISJ, WHITELIST_CONJ, BLACKLIST;
	}

	WhitelistMode whitelistMode;
	List<Pair<ItemAttribute, Boolean>> selectedAttributes;

	public AttributeFilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public AttributeFilterMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, ItemStack stack) {
		super(type, id, inv, stack);
	}

	public static AttributeFilterMenu create(int id, PlayerInventory inv, ItemStack stack) {
		return new AttributeFilterMenu(AllMenuTypes.ATTRIBUTE_FILTER.get(), id, inv, stack);
	}

	public void appendSelectedAttribute(ItemAttribute itemAttribute, boolean inverted) {
		selectedAttributes.add(Pair.of(itemAttribute, inverted));
	}

	@Override
	protected void init(PlayerInventory inv, ItemStack contentHolder) {
		super.init(inv, contentHolder);
		ItemStack stack = new ItemStack(Items.NAME_TAG);
		stack.setCustomName(
				Components.literal("Selected Tags").formatted(Formatting.RESET, Formatting.BLUE));
		ghostInventory.setStackInSlot(1, stack);
	}

	@Override
	protected int getPlayerInventoryXOffset() {
		return 51;
	}

	@Override
	protected int getPlayerInventoryYOffset() {
		return 107;
	}

	@Override
	protected void addFilterSlots() {
		this.addSlot(new SlotItemHandler(ghostInventory, 0, 16, 24));
		this.addSlot(new SlotItemHandler(ghostInventory, 1, 22, 59) {
			@Override
			public boolean canTakeItems(PlayerEntity playerIn) {
				return false;
			}
		});
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return new ItemStackHandler(2);
	}

	@Override
	public void clearContents() {
		selectedAttributes.clear();
	}

	@Override
	public void onSlotClick(int slotId, int dragType, SlotActionType clickTypeIn, PlayerEntity player) {
		if (slotId == 37)
			return;
		super.onSlotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean canInsertIntoSlot(Slot slotIn) {
		if (slotIn.id == 37)
			return false;
		return super.canInsertIntoSlot(slotIn);
	}

	@Override
	public boolean canInsertIntoSlot(ItemStack stack, Slot slotIn) {
		if (slotIn.id == 37)
			return false;
		return super.canInsertIntoSlot(stack, slotIn);
	}

	@Override
	public ItemStack quickMove(PlayerEntity playerIn, int index) {
		if (index == 37)
			return ItemStack.EMPTY;
		if (index == 36) {
			ghostInventory.setStackInSlot(37, ItemStack.EMPTY);
			return ItemStack.EMPTY;
		}
		if (index < 36) {
			ItemStack stackToInsert = playerInventory.getStack(index);
			ItemStack copy = stackToInsert.copy();
			copy.setCount(1);
			ghostInventory.setStackInSlot(0, copy);
		}
		return ItemStack.EMPTY;
	}

	@Override
	protected void initAndReadInventory(ItemStack filterItem) {
		super.initAndReadInventory(filterItem);
		selectedAttributes = new ArrayList<>();
		whitelistMode = WhitelistMode.values()[filterItem.getOrCreateNbt()
			.getInt("WhitelistMode")];
		NbtList attributes = filterItem.getOrCreateNbt()
			.getList("MatchedAttributes", NbtElement.COMPOUND_TYPE);
		attributes.forEach(inbt -> {
			NbtCompound compound = (NbtCompound) inbt;
			selectedAttributes.add(Pair.of(ItemAttribute.fromNBT(compound), compound.getBoolean("Inverted")));
		});
	}

	@Override
	protected void saveData(ItemStack filterItem) {
		filterItem.getOrCreateNbt()
				.putInt("WhitelistMode", whitelistMode.ordinal());
		NbtList attributes = new NbtList();
		selectedAttributes.forEach(at -> {
			if (at == null)
				return;
			NbtCompound compoundNBT = new NbtCompound();
			at.getFirst()
					.serializeNBT(compoundNBT);
			compoundNBT.putBoolean("Inverted", at.getSecond());
			attributes.add(compoundNBT);
		});
		filterItem.getOrCreateNbt()
			.put("MatchedAttributes", attributes);
		
		if (attributes.isEmpty() && whitelistMode == WhitelistMode.WHITELIST_DISJ)
			filterItem.setNbt(null);
	}

}

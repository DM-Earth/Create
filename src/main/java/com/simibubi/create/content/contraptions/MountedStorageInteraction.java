package com.simibubi.create.content.contraptions;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class MountedStorageInteraction {

	public static final List<ScreenHandlerType<?>> menus = ImmutableList.of(ScreenHandlerType.GENERIC_9X1, ScreenHandlerType.GENERIC_9X2,
		ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X4, ScreenHandlerType.GENERIC_9X5, ScreenHandlerType.GENERIC_9X6);

	public static NamedScreenHandlerFactory createMenuProvider(Text displayName, ItemStackHandler primary, @Nullable ItemStackHandler secondary,
		int slotCount, Supplier<Boolean> stillValid) {
		int rows = MathHelper.clamp(slotCount / 9, 1, 6);
		ScreenHandlerType<?> menuType = menus.get(rows - 1);
		Text menuName = Lang.translateDirect("contraptions.moving_container", displayName);

		return new NamedScreenHandlerFactory() {

			@Override
			public ScreenHandler createMenu(int pContainerId, PlayerInventory pPlayerInventory, PlayerEntity pPlayer) {
				return new GenericContainerScreenHandler(menuType, pContainerId, pPlayerInventory, new StorageInteractionContainer(primary, secondary, stillValid),
					rows);
			}

			@Override
			public Text getDisplayName() {
				return menuName;
			}

		};
	}

	public static class StorageInteractionContainer implements Inventory {

		private Supplier<Boolean> stillValid;
		private final ItemStackHandler primary;
		@Nullable
		private final ItemStackHandler secondary; // for double chests


		public StorageInteractionContainer(ItemStackHandler primary, @Nullable ItemStackHandler secondary, Supplier<Boolean> stillValid) {
			this.primary = primary;
			this.secondary = secondary;
			this.stillValid = stillValid;
		}

		private ItemStackHandler handlerForSlot(int slot) {
			return secondary == null || slot < primary.getSlotCount() ? primary : secondary;
		}

		private int actualSlot(int slot) {
			return handlerForSlot(slot) == primary ? slot : slot - primary.getSlotCount();
		}

		private boolean oob(int slot) {
			if (slot < 0)
				return true;
			ItemStackHandler handler = handlerForSlot(slot);
			slot = actualSlot(slot);
			return slot >= handler.getSlotCount();
		}

		@Override
		public int size() {
			return primary.getSlotCount() + (secondary == null ? 0 : secondary.getSlotCount());
		}

		@Override
		public boolean isEmpty() {
			boolean primaryEmpty = Iterators.size(primary.nonEmptyIterator()) == 0;
			boolean secondaryEmpty = secondary == null || Iterators.size(secondary.nonEmptyIterator()) == 0;
			return primaryEmpty && secondaryEmpty;
		}

		@Override
		public ItemStack getStack(int slot) {
			if (oob(slot))
				return ItemStack.EMPTY;
			ItemStackHandler handler = handlerForSlot(slot);
			slot = actualSlot(slot);
			return handler.getStackInSlot(slot);
		}

		@Override
		public ItemStack removeStack(int slot, int count) {
			if (oob(slot))
				return ItemStack.EMPTY;
			ItemStackHandler handler = handlerForSlot(slot);
			slot = actualSlot(slot);

			ItemStack current = handler.getStackInSlot(slot);
			if (current.isEmpty())
				return ItemStack.EMPTY;
			current = current.copy();
			ItemStack extracted = current.split(count);
			handler.setStackInSlot(slot, current);
			return extracted;
		}

		@Override
		public ItemStack removeStack(int slot) {
			return removeStack(slot, Integer.MAX_VALUE);
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
			if (!oob(slot)) {
				ItemStackHandler handler = handlerForSlot(slot);
				slot = actualSlot(slot);
				handler.setStackInSlot(slot, stack.copy());
			}
		}

		@Override
		public void markDirty() {
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			return stillValid.get();
		}

		@Override
		public boolean isValid(int slot, ItemStack stack) {
			ItemStackHandler handler = handlerForSlot(slot);
			slot = actualSlot(slot);
			return handler.isItemValid(slot, ItemVariant.of(stack), 1);
		}

		@Override
		public void clear() {
			primary.setSize(primary.getSlotCount());
			if (secondary != null)
				secondary.setSize(secondary.getSlotCount());
		}
	}

}

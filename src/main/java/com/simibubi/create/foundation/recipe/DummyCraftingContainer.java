package com.simibubi.create.foundation.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

public class DummyCraftingContainer extends CraftingInventory {

	private final DefaultedList<ItemStack> inv;

	public DummyCraftingContainer(DefaultedList<ItemStack> stacks) {
		super(null, 0, 0);
		this.inv = stacks;
	}

	@Override
	public int size() {
		return this.inv.size();
	}

	@Override
	public boolean isEmpty() {
		for (int slot = 0; slot < this.size(); slot++) {
			if (!this.getStack(slot).isEmpty())
				return false;
		}

		return true;
	}

	@Override
	public @NotNull ItemStack getStack(int slot) {
		return slot >= this.size() ? ItemStack.EMPTY : this.inv.get(slot);
	}

	@Override
	public @NotNull ItemStack removeStack(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public @NotNull ItemStack removeStack(int slot, int count) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setStack(int slot, @NotNull ItemStack stack) {}

	@Override
	public void clear() {}

	@Override
	public void provideRecipeInputs(@NotNull RecipeMatcher helper) {}
}

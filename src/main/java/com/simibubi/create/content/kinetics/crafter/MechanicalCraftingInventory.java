package com.simibubi.create.content.kinetics.crafter;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class MechanicalCraftingInventory extends CraftingInventory {

	private static final ScreenHandler dummyContainer = new ScreenHandler(null, -1) {
		public boolean canUse(PlayerEntity playerIn) {
			return false;
		}

		@Override
		public ItemStack quickMove(PlayerEntity p_38941_, int p_38942_) {
			return ItemStack.EMPTY;
		}
	};

	public MechanicalCraftingInventory(GroupedItems items) {
		super(dummyContainer, items.width, items.height);
		for (int y = 0; y < items.height; y++) {
			for (int x = 0; x < items.width; x++) {
				ItemStack stack = items.grid.get(Pair.of(x + items.minX, y + items.minY));
				setStack(x + (items.height - y - 1) * items.width,
						stack == null ? ItemStack.EMPTY : stack.copy());
			}
		}
	}

}

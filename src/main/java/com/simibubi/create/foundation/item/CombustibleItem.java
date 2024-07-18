package com.simibubi.create.foundation.item;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;

public class CombustibleItem extends Item {
	private int burnTime = -1;

	public CombustibleItem(Settings properties) {
		super(properties);
	}

	public void setBurnTime(int burnTime) {
		FuelRegistry.INSTANCE.add(this, burnTime);
		this.burnTime = burnTime;
	}

//	@Override
	public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
		return this.burnTime;
	}

}

package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.World;

public class EmptyingRecipe extends ProcessingRecipe<Inventory> {

	public EmptyingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.EMPTYING, params);
	}

	@Override
	public boolean matches(Inventory inv, World p_77569_2_) {
		return ingredients.get(0).test(inv.getStack(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}

	public FluidStack getResultingFluid() {
		if (fluidResults.isEmpty())
			throw new IllegalStateException("Emptying Recipe: " + id.toString() + " has no fluid output!");
		return fluidResults.get(0);
	}

}
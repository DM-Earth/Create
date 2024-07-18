package com.simibubi.create.content.fluids.transfer;

import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.FluidUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class FillingRecipe extends ProcessingRecipe<Inventory> implements IAssemblyRecipe {

	public FillingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.FILLING, params);
	}

	@Override
	public boolean matches(Inventory inv, World p_77569_2_) {
		return ingredients.get(0)
				.test(inv.getStack(0));
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
	protected int getMaxFluidInputCount() {
		return 1;
	}

	public FluidIngredient getRequiredFluid() {
		if (fluidIngredients.isEmpty())
			throw new IllegalStateException("Filling Recipe: " + id.toString() + " has no fluid ingredient!");
		return fluidIngredients.get(0);
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {
	}

	@Override
	public void addAssemblyFluidIngredients(List<FluidIngredient> list) {
		list.add(getRequiredFluid());
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Text getDescriptionForAssembly() {
		List<FluidStack> matchingFluidStacks = fluidIngredients.get(0).getMatchingFluidStacks();

		if (matchingFluidStacks.size() == 0)
			return Components.literal("Invalid");

		Fluid fluid = matchingFluidStacks.get(0).getFluid();
		String translationKey = FluidUtil.getTranslationKey(fluid);
		return Lang.translateDirect("recipe.assembly.spout_filling_fluid", Text.translatable(translationKey).getString());
	}

	@Override
	public void addRequiredMachines(Set<ItemConvertible> list) {
		list.add(AllBlocks.SPOUT.get());
	}

	@Override
	public SequencedAssemblySubCategoryType getJEISubCategory() {
		return SequencedAssemblySubCategoryType.SPOUTING;
	}

}

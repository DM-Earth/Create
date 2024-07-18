package com.simibubi.create.content.kinetics.press;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.world.World;

@ParametersAreNonnullByDefault
public class PressingRecipe extends ProcessingRecipe<Inventory> implements IAssemblyRecipe {

	public PressingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.PRESSING, params);
	}

	@Override
	public boolean matches(Inventory inv, World worldIn) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0)
			.test(inv.getStack(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 2;
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	@Environment(EnvType.CLIENT)
	public Text getDescriptionForAssembly() {
		return Lang.translateDirect("recipe.assembly.pressing");
	}

	@Override
	public void addRequiredMachines(Set<ItemConvertible> list) {
		list.add(AllBlocks.MECHANICAL_PRESS.get());
	}

	@Override
	public SequencedAssemblySubCategoryType getJEISubCategory() {
		return SequencedAssemblySubCategoryType.PRESSING;
	}

}

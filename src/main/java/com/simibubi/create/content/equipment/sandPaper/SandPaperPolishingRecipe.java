package com.simibubi.create.content.equipment.sandPaper;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe.SandPaperInv;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerContainer;

@ParametersAreNonnullByDefault
public class SandPaperPolishingRecipe extends ProcessingRecipe<SandPaperInv> {

	public SandPaperPolishingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.SANDPAPER_POLISHING, params);
	}

	@Override
	public boolean matches(SandPaperInv inv, World worldIn) {
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

	public static boolean canPolish(World world, ItemStack stack) {
		return !getMatchingRecipes(world, stack).isEmpty();
	}

	public static ItemStack applyPolish(World world, Vec3d position, ItemStack stack, ItemStack sandPaperStack) {
		List<Recipe<SandPaperInv>> matchingRecipes = getMatchingRecipes(world, stack);
		if (!matchingRecipes.isEmpty())
			return matchingRecipes.get(0)
				.craft(new SandPaperInv(stack), world.getRegistryManager())
				.copy();
		return stack;
	}

	public static List<Recipe<SandPaperInv>> getMatchingRecipes(World world, ItemStack stack) {
		return world.getRecipeManager()
			.getAllMatches(AllRecipeTypes.SANDPAPER_POLISHING.getType(), new SandPaperInv(stack), world);
	}

	public static class SandPaperInv extends ItemStackHandlerContainer {

		public SandPaperInv(ItemStack stack) {
			super(1);
			setStackInSlot(0, stack);
		}

	}

}

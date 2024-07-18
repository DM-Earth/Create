package com.simibubi.create.compat.rei;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

/**
 * Helper recipe type for displaying an item relationship in JEI
 */
@ParametersAreNonnullByDefault
public class ConversionRecipe extends ProcessingRecipe<Inventory> {

	static int counter = 0;

	public static ConversionRecipe create(ItemStack from, ItemStack to) {
		Identifier recipeId = Create.asResource("conversion_" + counter++);
		return new ProcessingRecipeBuilder<>(ConversionRecipe::new, recipeId)
			.withItemIngredients(Ingredient.ofStacks(from))
			.withSingleItemOutput(to)
			.build();
	}

	public ConversionRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.CONVERSION, params);
	}

	@Override
	public boolean matches(Inventory inv, World worldIn) {
		return false;
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

}

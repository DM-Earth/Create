package com.simibubi.create.compat.emi.recipes.basin;

import com.simibubi.create.content.processing.basin.BasinRecipe;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.util.Identifier;

public class ShapelessEmiRecipe extends MixingEmiRecipe {

	public ShapelessEmiRecipe(EmiRecipeCategory category, BasinRecipe recipe) {
		super(category, recipe);
		Identifier id = recipe.getId();
		this.id = new Identifier ("emi", "create/shapeless/" + id.getNamespace() + "/" + id.getPath());
	}
}

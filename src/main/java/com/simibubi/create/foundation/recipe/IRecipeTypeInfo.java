package com.simibubi.create.foundation.recipe;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;

public interface IRecipeTypeInfo {

	Identifier getId();

	<T extends RecipeSerializer<?>> T getSerializer();

	<T extends RecipeType<?>> T getType();

}

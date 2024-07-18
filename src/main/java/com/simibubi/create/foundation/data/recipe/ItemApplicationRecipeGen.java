package com.simibubi.create.foundation.data.recipe;

import java.util.function.Supplier;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllItemTags;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

public class ItemApplicationRecipeGen extends ProcessingRecipeGen {

	GeneratedRecipe ANDESITE = woodCasing("andesite", I::andesite, I::andesiteCasing);
	GeneratedRecipe COPPER = woodCasing("copper", I::copper, I::copperCasing);
	GeneratedRecipe BRASS = woodCasingTag("brass", I::brass, I::brassCasing);
	GeneratedRecipe RAILWAY = create("railway_casing", b -> b.require(I.brassCasing())
		.require(I.sturdySheet())
		.output(I.railwayCasing()));

	protected GeneratedRecipe woodCasing(String type, Supplier<ItemConvertible> ingredient, Supplier<ItemConvertible> output) {
		return woodCasingIngredient(type, () -> Ingredient.ofItems(ingredient.get()), output);
	}

	protected GeneratedRecipe woodCasingTag(String type, Supplier<TagKey<Item>> ingredient, Supplier<ItemConvertible> output) {
		return woodCasingIngredient(type, () -> Ingredient.fromTag(ingredient.get()), output);
	}

	protected GeneratedRecipe woodCasingIngredient(String type, Supplier<Ingredient> ingredient,
		Supplier<ItemConvertible> output) {
		create(type + "_casing_from_log", b -> b.require(AllItemTags.STRIPPED_LOGS.tag)
			.require(ingredient.get())
			.output(output.get()));
		return create(type + "_casing_from_wood", b -> b.require(AllItemTags.STRIPPED_WOOD.tag)
			.require(ingredient.get())
			.output(output.get()));
	}

	public ItemApplicationRecipeGen(FabricDataOutput output) {
		super(output);
	}

	@Override
	protected AllRecipeTypes getRecipeType() {
		return AllRecipeTypes.ITEM_APPLICATION;
	}

}

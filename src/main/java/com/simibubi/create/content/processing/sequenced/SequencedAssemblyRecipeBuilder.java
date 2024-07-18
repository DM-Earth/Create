package com.simibubi.create.content.processing.sequenced;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeFactory;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class SequencedAssemblyRecipeBuilder {

	private SequencedAssemblyRecipe recipe;
	protected List<ConditionJsonProvider> recipeConditions;

	public SequencedAssemblyRecipeBuilder(Identifier id) {
		recipeConditions = new ArrayList<>();
		this.recipe = new SequencedAssemblyRecipe(id,
			AllRecipeTypes.SEQUENCED_ASSEMBLY.getSerializer());
	}

	public <T extends ProcessingRecipe<?>> SequencedAssemblyRecipeBuilder addStep(ProcessingRecipeFactory<T> factory,
		UnaryOperator<ProcessingRecipeBuilder<T>> builder) {
		ProcessingRecipeBuilder<T> recipeBuilder =
			new ProcessingRecipeBuilder<>(factory, new Identifier("dummy"));
		Item placeHolder = recipe.getTransitionalItem()
			.getItem();
		recipe.getSequence()
			.add(new SequencedRecipe<>(builder.apply(recipeBuilder.require(placeHolder)
				.output(placeHolder))
				.build()));
		return this;
	}

	public SequencedAssemblyRecipeBuilder require(ItemConvertible ingredient) {
		return require(Ingredient.ofItems(ingredient));
	}

	public SequencedAssemblyRecipeBuilder require(TagKey<Item> tag) {
		return require(Ingredient.fromTag(tag));
	}

	public SequencedAssemblyRecipeBuilder require(Ingredient ingredient) {
		recipe.ingredient = ingredient;
		return this;
	}

	public SequencedAssemblyRecipeBuilder transitionTo(ItemConvertible item) {
		recipe.transitionalItem = new ProcessingOutput(new ItemStack(item), 1);
		return this;
	}

	public SequencedAssemblyRecipeBuilder loops(int loops) {
		recipe.loops = loops;
		return this;
	}

	public SequencedAssemblyRecipeBuilder addOutput(ItemConvertible item, float weight) {
		return addOutput(new ItemStack(item), weight);
	}

	public SequencedAssemblyRecipeBuilder addOutput(ItemStack item, float weight) {
		recipe.resultPool.add(new ProcessingOutput(item, weight));
		return this;
	}

	public SequencedAssemblyRecipe build() {
		return recipe;
	}

	public void build(Consumer<RecipeJsonProvider> consumer) {
		consumer.accept(new DataGenResult(build(), recipeConditions));
	}

	public static class DataGenResult implements RecipeJsonProvider {

		private SequencedAssemblyRecipe recipe;
		private List<ConditionJsonProvider> recipeConditions;
		private Identifier id;
		private SequencedAssemblyRecipeSerializer serializer;

		public DataGenResult(SequencedAssemblyRecipe recipe, List<ConditionJsonProvider> recipeConditions) {
			this.recipeConditions = recipeConditions;
			this.recipe = recipe;
			this.id = new Identifier(recipe.getId().getNamespace(),
					AllRecipeTypes.SEQUENCED_ASSEMBLY.getId().getPath() + "/" + recipe.getId().getPath());
			this.serializer = (SequencedAssemblyRecipeSerializer) recipe.getSerializer();
		}

		@Override
		public void serialize(JsonObject json) {
			serializer.write(json, recipe);
			if (recipeConditions.isEmpty())
				return;

			JsonArray conds = new JsonArray();
			recipeConditions.forEach(c -> conds.add(c.toJson()));
			json.add(ResourceConditions.CONDITIONS_KEY, conds);
		}

		@Override
		public Identifier getRecipeId() {
			return id;
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return serializer;
		}

		@Override
		public JsonObject toAdvancementJson() {
			return null;
		}

		@Override
		public Identifier getAdvancementId() {
			return null;
		}

	}

}

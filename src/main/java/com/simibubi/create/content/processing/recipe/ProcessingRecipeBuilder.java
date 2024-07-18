package com.simibubi.create.content.processing.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.Pair;
import com.tterrag.registrate.util.DataIngredient;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public class ProcessingRecipeBuilder<T extends ProcessingRecipe<?>> {

	protected ProcessingRecipeFactory<T> factory;
	protected ProcessingRecipeParams params;
	protected List<ConditionJsonProvider> recipeConditions;

	public ProcessingRecipeBuilder(ProcessingRecipeFactory<T> factory, Identifier recipeId) {
		params = new ProcessingRecipeParams(recipeId);
		recipeConditions = new ArrayList<>();
		this.factory = factory;
	}

	public ProcessingRecipeBuilder<T> withItemIngredients(Ingredient... ingredients) {
		return withItemIngredients(DefaultedList.copyOf(Ingredient.EMPTY, ingredients));
	}

	public ProcessingRecipeBuilder<T> withItemIngredients(DefaultedList<Ingredient> ingredients) {
		params.ingredients = ingredients;
		return this;
	}

	public ProcessingRecipeBuilder<T> withSingleItemOutput(ItemStack output) {
		return withItemOutputs(new ProcessingOutput(output, 1));
	}

	public ProcessingRecipeBuilder<T> withItemOutputs(ProcessingOutput... outputs) {
		return withItemOutputs(DefaultedList.copyOf(ProcessingOutput.EMPTY, outputs));
	}

	public ProcessingRecipeBuilder<T> withItemOutputs(DefaultedList<ProcessingOutput> outputs) {
		params.results = outputs;
		return this;
	}

	public ProcessingRecipeBuilder<T> withFluidIngredients(FluidIngredient... ingredients) {
		return withFluidIngredients(DefaultedList.copyOf(FluidIngredient.EMPTY, ingredients));
	}

	public ProcessingRecipeBuilder<T> withFluidIngredients(DefaultedList<FluidIngredient> ingredients) {
		params.fluidIngredients = ingredients;
		return this;
	}

	public ProcessingRecipeBuilder<T> withFluidOutputs(FluidStack... outputs) {
		return withFluidOutputs(DefaultedList.copyOf(FluidStack.EMPTY, outputs));
	}

	public ProcessingRecipeBuilder<T> withFluidOutputs(DefaultedList<FluidStack> outputs) {
		params.fluidResults = outputs;
		return this;
	}

	public ProcessingRecipeBuilder<T> duration(int ticks) {
		params.processingDuration = ticks;
		return this;
	}

	public ProcessingRecipeBuilder<T> averageProcessingDuration() {
		return duration(100);
	}

	public ProcessingRecipeBuilder<T> requiresHeat(HeatCondition condition) {
		params.requiredHeat = condition;
		return this;
	}

	public T build() {
		validateFluidAmounts();
		return factory.create(params);
	}

	public void build(Consumer<RecipeJsonProvider> consumer) {
		consumer.accept(new DataGenResult<>(build(), recipeConditions));
	}

	public static final long[] SUS_AMOUNTS = { 10, 250, 500, 1000 };

	private void validateFluidAmounts() {
		for (FluidIngredient ingredient : params.fluidIngredients) {
			for (long amount : SUS_AMOUNTS) {
				if (ingredient.getRequiredAmount() == amount) {
					Create.LOGGER.warn("Suspicious fluid amount in recipe [{}]: {}", params.id, amount);
				}
			}
		}
	}

	// Datagen shortcuts

	public ProcessingRecipeBuilder<T> require(TagKey<Item> tag) {
		return require(Ingredient.fromTag(tag));
	}

	public ProcessingRecipeBuilder<T> require(ItemConvertible item) {
		return require(Ingredient.ofItems(item));
	}

	public ProcessingRecipeBuilder<T> require(Ingredient ingredient) {
		params.ingredients.add(ingredient);
		return this;
	}

	// fabric: custom ingredient support
	public ProcessingRecipeBuilder<T> require(CustomIngredient ingredient) {
		return require(ingredient.toVanilla());
	}

	public ProcessingRecipeBuilder<T> require(Mods mod, String id) {
		params.ingredients.add(new SimpleDatagenIngredient(mod, id));
		return this;
	}

	public ProcessingRecipeBuilder<T> require(Identifier ingredient) {
		params.ingredients.add(DataIngredient.ingredient(null, ingredient));
		return this;
	}

	public ProcessingRecipeBuilder<T> require(Fluid fluid, long amount) {
		return require(FluidIngredient.fromFluid(fluid, amount));
	}

	public ProcessingRecipeBuilder<T> require(TagKey<Fluid> fluidTag, long amount) {
		return require(FluidIngredient.fromTag(fluidTag, amount));
	}

	public ProcessingRecipeBuilder<T> require(FluidIngredient ingredient) {
		params.fluidIngredients.add(ingredient);
		return this;
	}

	public ProcessingRecipeBuilder<T> output(ItemConvertible item) {
		return output(item, 1);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemConvertible item) {
		return output(chance, item, 1);
	}

	public ProcessingRecipeBuilder<T> output(ItemConvertible item, int amount) {
		return output(1, item, amount);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemConvertible item, int amount) {
		return output(chance, new ItemStack(item, amount));
	}

	public ProcessingRecipeBuilder<T> output(ItemStack output) {
		return output(1, output);
	}

	public ProcessingRecipeBuilder<T> output(float chance, ItemStack output) {
		return output(new ProcessingOutput(output, chance));
	}

	public ProcessingRecipeBuilder<T> output(float chance, Mods mod, String id, int amount) {
		return output(new ProcessingOutput(Pair.of(mod.asResource(id), amount), chance));
	}

	public ProcessingRecipeBuilder<T> output(float chance, Identifier registryName, int amount) {
		return output(new ProcessingOutput(Pair.of(registryName, amount), chance));
	}

	public ProcessingRecipeBuilder<T> output(ProcessingOutput output) {
		params.results.add(output);
		return this;
	}

	public ProcessingRecipeBuilder<T> output(Fluid fluid, long amount) {
		fluid = FluidHelper.convertToStill(fluid);
		return output(new FluidStack(fluid, amount));
	}

	public ProcessingRecipeBuilder<T> output(FluidStack fluidStack) {
		params.fluidResults.add(fluidStack);
		return this;
	}

	public ProcessingRecipeBuilder<T> toolNotConsumed() {
		params.keepHeldItem = true;
		return this;
	}

	//

	public ProcessingRecipeBuilder<T> whenModLoaded(String modid) {
		return withCondition(DefaultResourceConditions.allModsLoaded(modid));
	}

	public ProcessingRecipeBuilder<T> whenModMissing(String modid) {
		return withCondition(DefaultResourceConditions.not(DefaultResourceConditions.allModsLoaded(modid)));
	}

	public ProcessingRecipeBuilder<T> withCondition(ConditionJsonProvider condition) {
		recipeConditions.add(condition);
		return this;
	}

	@FunctionalInterface
	public interface ProcessingRecipeFactory<T extends ProcessingRecipe<?>> {
		T create(ProcessingRecipeParams params);
	}

	public static class ProcessingRecipeParams {

		protected Identifier id;
		protected DefaultedList<Ingredient> ingredients;
		protected DefaultedList<ProcessingOutput> results;
		protected DefaultedList<FluidIngredient> fluidIngredients;
		protected DefaultedList<FluidStack> fluidResults;
		protected int processingDuration;
		protected HeatCondition requiredHeat;

		public boolean keepHeldItem;

		protected ProcessingRecipeParams(Identifier id) {
			this.id = id;
			ingredients = DefaultedList.of();
			results = DefaultedList.of();
			fluidIngredients = DefaultedList.of();
			fluidResults = DefaultedList.of();
			processingDuration = 0;
			requiredHeat = HeatCondition.NONE;
			keepHeldItem = false;
		}

	}

	public static class DataGenResult<S extends ProcessingRecipe<?>> implements RecipeJsonProvider {

		private List<ConditionJsonProvider> recipeConditions;
		private ProcessingRecipeSerializer<S> serializer;
		private Identifier id;
		private S recipe;

		@SuppressWarnings("unchecked")
		public DataGenResult(S recipe, List<ConditionJsonProvider> recipeConditions) {
			this.recipe = recipe;
			this.recipeConditions = recipeConditions;
			IRecipeTypeInfo recipeType = this.recipe.getTypeInfo();
			Identifier typeId = recipeType.getId();

			if (!(recipeType.getSerializer() instanceof ProcessingRecipeSerializer))
				throw new IllegalStateException("Cannot datagen ProcessingRecipe of type: " + typeId);

			this.id = new Identifier(recipe.getId().getNamespace(),
					typeId.getPath() + "/" + recipe.getId().getPath());
			this.serializer = (ProcessingRecipeSerializer<S>) recipe.getSerializer();
		}

		@Override
		public void serialize(JsonObject json) {
			serializer.write(json, recipe);
			if (recipeConditions.isEmpty())
				return;

			JsonArray conds = new JsonArray();
			recipeConditions.forEach(c -> conds.add(c.toJson())); // FabricDataGenHelper.addConditions(json, recipeConditions.toArray());?
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

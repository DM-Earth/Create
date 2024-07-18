package com.simibubi.create.foundation.data.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.simibubi.create.Create;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

public abstract class ProcessingRecipeGen extends CreateRecipeProvider {

	protected static final List<ProcessingRecipeGen> GENERATORS = new ArrayList<>();
	protected static final long BUCKET = FluidConstants.BUCKET;
	protected static final long BOTTLE = FluidConstants.BOTTLE;

	public static DataProvider registerAll(FabricDataOutput output) {
		GENERATORS.add(new CrushingRecipeGen(output));
		GENERATORS.add(new MillingRecipeGen(output));
		GENERATORS.add(new CuttingRecipeGen(output));
		GENERATORS.add(new WashingRecipeGen(output));
		GENERATORS.add(new PolishingRecipeGen(output));
		GENERATORS.add(new DeployingRecipeGen(output));
		GENERATORS.add(new MixingRecipeGen(output));
		GENERATORS.add(new CompactingRecipeGen(output));
		GENERATORS.add(new PressingRecipeGen(output));
		GENERATORS.add(new FillingRecipeGen(output));
		GENERATORS.add(new EmptyingRecipeGen(output));
		GENERATORS.add(new HauntingRecipeGen(output));
		GENERATORS.add(new ItemApplicationRecipeGen(output));

		return new DataProvider() {

			@Override
			public String getName() {
				return "Create's Processing Recipes";
			}

			@Override
			public CompletableFuture<?> run(DataWriter dc) {
				return CompletableFuture.allOf(GENERATORS.stream()
					.map(gen -> gen.run(dc))
					.toArray(CompletableFuture[]::new));
			}
		};
	}

	public ProcessingRecipeGen(FabricDataOutput generator) {
		super(generator);
	}

	/**
	 * Create a processing recipe with a single itemstack ingredient, using its id
	 * as the name of the recipe
	 */
	protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(String namespace,
		Supplier<ItemConvertible> singleIngredient, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		ProcessingRecipeSerializer<T> serializer = getSerializer();
		GeneratedRecipe generatedRecipe = c -> {
			ItemConvertible itemLike = singleIngredient.get();
			transform
				.apply(new ProcessingRecipeBuilder<>(serializer.getFactory(),
					new Identifier(namespace, RegisteredObjects.getKeyOrThrow(itemLike.asItem())
						.getPath())).withItemIngredients(Ingredient.ofItems(itemLike)))
				.build(c);
		};
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	/**
	 * Create a processing recipe with a single itemstack ingredient, using its id
	 * as the name of the recipe
	 */
	<T extends ProcessingRecipe<?>> GeneratedRecipe create(Supplier<ItemConvertible> singleIngredient,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		return create(Create.ID, singleIngredient, transform);
	}

	protected <T extends ProcessingRecipe<?>> GeneratedRecipe createWithDeferredId(Supplier<Identifier> name,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		ProcessingRecipeSerializer<T> serializer = getSerializer();
		GeneratedRecipe generatedRecipe =
			c -> transform.apply(new ProcessingRecipeBuilder<>(serializer.getFactory(), name.get()))
				.build(c);
		all.add(generatedRecipe);
		return generatedRecipe;
	}

	/**
	 * Create a new processing recipe, with recipe definitions provided by the
	 * function
	 */
	protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(Identifier name,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		return createWithDeferredId(() -> name, transform);
	}

	/**
	 * Create a new processing recipe, with recipe definitions provided by the
	 * function
	 */
	<T extends ProcessingRecipe<?>> GeneratedRecipe create(String name,
		UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
		return create(Create.asResource(name), transform);
	}

	protected abstract IRecipeTypeInfo getRecipeType();

	protected <T extends ProcessingRecipe<?>> ProcessingRecipeSerializer<T> getSerializer() {
		return getRecipeType().getSerializer();
	}

	protected Supplier<Identifier> idWithSuffix(Supplier<ItemConvertible> item, String suffix) {
		return () -> {
			Identifier registryName = RegisteredObjects.getKeyOrThrow(item.get()
				.asItem());
			return Create.asResource(registryName.getPath() + suffix);
		};
	}

	@Override
	public String getName() {
		return "Create's Processing Recipes: " + getRecipeType().getId()
			.getPath();
	}

}

package com.simibubi.create.foundation.data.recipe;

import java.util.function.UnaryOperator;

import com.google.common.base.Supplier;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class MechanicalCraftingRecipeGen extends CreateRecipeProvider {

	GeneratedRecipe

	CRUSHING_WHEEL = create(AllBlocks.CRUSHING_WHEEL::get).returns(2)
		.recipe(b -> b.key('P', Ingredient.fromTag(ItemTags.PLANKS))
			.key('S', Ingredient.fromTag(I.stone()))
			.key('A', I.andesite())
			.patternLine(" AAA ")
			.patternLine("AAPAA")
			.patternLine("APSPA")
			.patternLine("AAPAA")
			.patternLine(" AAA ")
			.disallowMirrored()),

		WAND_OF_SYMMETRY =
			create(AllItems.WAND_OF_SYMMETRY::get).recipe(b -> b.key('E', Ingredient.fromTag(Tags.Items.ENDER_PEARLS))
				.key('G', Ingredient.fromTag(Tags.Items.GLASS))
				.key('P', I.precisionMechanism())
				.key('O', Ingredient.fromTag(Tags.Items.OBSIDIAN))
				.key('B', Ingredient.fromTag(I.brass()))
				.patternLine(" G ")
				.patternLine("GEG")
				.patternLine(" P ")
				.patternLine(" B ")
				.patternLine(" O ")),

		EXTENDO_GRIP = create(AllItems.EXTENDO_GRIP::get).returns(1)
			.recipe(b -> b.key('L', Ingredient.fromTag(I.brass()))
				.key('R', I.precisionMechanism())
				.key('H', AllItems.BRASS_HAND.get())
				.key('S', Ingredient.fromTag(Tags.Items.RODS_WOODEN))
				.patternLine(" L ")
				.patternLine(" R ")
				.patternLine("SSS")
				.patternLine("SSS")
				.patternLine(" H ")
				.disallowMirrored()),

		POTATO_CANNON = create(AllItems.POTATO_CANNON::get).returns(1)
			.recipe(b -> b.key('L', I.andesite())
				.key('R', I.precisionMechanism())
				.key('S', AllBlocks.FLUID_PIPE.get())
				.key('C', Ingredient.ofItems(I.copper()))
				.patternLine("LRSSS")
				.patternLine("CC   "))

	;

	public MechanicalCraftingRecipeGen(FabricDataOutput p_i48262_1_) {
		super(p_i48262_1_);
	}

	GeneratedRecipeBuilder create(Supplier<ItemConvertible> result) {
		return new GeneratedRecipeBuilder(result);
	}

	class GeneratedRecipeBuilder {

		private String suffix;
		private Supplier<ItemConvertible> result;
		private int amount;

		public GeneratedRecipeBuilder(Supplier<ItemConvertible> result) {
			this.suffix = "";
			this.result = result;
			this.amount = 1;
		}

		GeneratedRecipeBuilder returns(int amount) {
			this.amount = amount;
			return this;
		}

		GeneratedRecipeBuilder withSuffix(String suffix) {
			this.suffix = suffix;
			return this;
		}

		GeneratedRecipe recipe(UnaryOperator<MechanicalCraftingRecipeBuilder> builder) {
			return register(consumer -> {
				MechanicalCraftingRecipeBuilder b =
					builder.apply(MechanicalCraftingRecipeBuilder.shapedRecipe(result.get(), amount));
				Identifier location = Create.asResource("mechanical_crafting/" + RegisteredObjects.getKeyOrThrow(result.get()
					.asItem())
					.getPath() + suffix);
				b.build(consumer, location);
			});
		}
	}

	@Override
	public String getName() {
		return "Create's Mechanical Crafting Recipes";
	}

}

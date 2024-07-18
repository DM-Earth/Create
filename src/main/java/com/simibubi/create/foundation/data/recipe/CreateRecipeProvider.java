package com.simibubi.create.foundation.data.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;

public abstract class CreateRecipeProvider extends FabricRecipeProvider {

	protected final List<GeneratedRecipe> all = new ArrayList<>();

	public CreateRecipeProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generate(Consumer<RecipeJsonProvider> p_200404_1_) {
		all.forEach(c -> c.register(p_200404_1_));
		Create.LOGGER.info(getName() + " registered " + all.size() + " recipe" + (all.size() == 1 ? "" : "s"));
	}

	protected GeneratedRecipe register(GeneratedRecipe recipe) {
		all.add(recipe);
		return recipe;
	}

	@FunctionalInterface
	public interface GeneratedRecipe {
		void register(Consumer<RecipeJsonProvider> consumer);
	}

	protected static class Marker {
	}

	protected static class I {

		static TagKey<Item> redstone() {
			return Tags.Items.DUSTS_REDSTONE;
		}

		static TagKey<Item> planks() {
			return ItemTags.PLANKS;
		}

		static TagKey<Item> woodSlab() {
			return ItemTags.WOODEN_SLABS;
		}

		static TagKey<Item> gold() {
			return AllTags.forgeItemTag("gold_ingots");
		}

		static TagKey<Item> goldSheet() {
			return AllTags.forgeItemTag("gold_plates");
		}

		static TagKey<Item> stone() {
			return Tags.Items.STONE;
		}

		static ItemConvertible andesite() {
			return AllItems.ANDESITE_ALLOY.get();
		}

		static ItemConvertible shaft() {
			return AllBlocks.SHAFT.get();
		}

		static ItemConvertible cog() {
			return AllBlocks.COGWHEEL.get();
		}

		static ItemConvertible largeCog() {
			return AllBlocks.LARGE_COGWHEEL.get();
		}

		static ItemConvertible andesiteCasing() {
			return AllBlocks.ANDESITE_CASING.get();
		}

		static TagKey<Item> brass() {
			return AllTags.forgeItemTag("brass_ingots");
		}

		static TagKey<Item> brassSheet() {
			return AllTags.forgeItemTag("brass_plates");
		}

		static TagKey<Item> iron() {
			return Tags.Items.INGOTS_IRON;
		}

		static TagKey<Item> ironNugget() {
			return AllTags.forgeItemTag("iron_nuggets");
		}

		static TagKey<Item> zinc() {
			return AllTags.forgeItemTag("zinc_ingots");
		}

		static TagKey<Item> ironSheet() {
			return AllTags.forgeItemTag("iron_plates");
		}

		static TagKey<Item> sturdySheet() {
			return AllTags.forgeItemTag("obsidian_plates");
		}

		static ItemConvertible brassCasing() {
			return AllBlocks.BRASS_CASING.get();
		}

		static ItemConvertible railwayCasing() {
			return AllBlocks.RAILWAY_CASING.get();
		}

		static ItemConvertible electronTube() {
			return AllItems.ELECTRON_TUBE.get();
		}

		static ItemConvertible precisionMechanism() {
			return AllItems.PRECISION_MECHANISM.get();
		}

		static ItemConvertible copperBlock() {
			return Items.COPPER_BLOCK;
		}

		static TagKey<Item> brassBlock() {
			return AllTags.forgeItemTag("brass_blocks");
		}

		static TagKey<Item> zincBlock() {
			return AllTags.forgeItemTag("zinc_blocks");
		}

		static TagKey<Item> wheatFlour() {
			return AllTags.forgeItemTag("wheat_flour");
		}

		static ItemConvertible copper() {
			return Items.COPPER_INGOT;
		}

		static TagKey<Item> copperSheet() {
			return AllTags.forgeItemTag("copper_plates");
		}

		static TagKey<Item> copperNugget() {
			return AllTags.forgeItemTag("copper_nuggets");
		}

		static TagKey<Item> brassNugget() {
			return AllTags.forgeItemTag("brass_nuggets");
		}

		static TagKey<Item> zincNugget() {
			return AllTags.forgeItemTag("zinc_nuggets");
		}

		static ItemConvertible copperCasing() {
			return AllBlocks.COPPER_CASING.get();
		}

		static ItemConvertible refinedRadiance() {
			return AllItems.REFINED_RADIANCE.get();
		}

		static ItemConvertible shadowSteel() {
			return AllItems.SHADOW_STEEL.get();
		}

		static Ingredient netherite() {
			return Ingredient.fromTag(AllTags.forgeItemTag("netherite_ingots"));
		}

	}
}

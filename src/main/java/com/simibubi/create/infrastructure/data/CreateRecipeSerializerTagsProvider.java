package com.simibubi.create.infrastructure.data;

import com.simibubi.create.AllTags.AllRecipeSerializerTags;
import com.simibubi.create.compat.Mods;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.server.tag.TagProvider;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import java.util.concurrent.CompletableFuture;

public class CreateRecipeSerializerTagsProvider extends TagProvider<RecipeSerializer<?>> {
	public CreateRecipeSerializerTagsProvider(FabricDataOutput generator, CompletableFuture<WrapperLookup> lookupProvider) {
		super(generator, RegistryKeys.RECIPE_SERIALIZER, lookupProvider);
	}

	@Override
	protected void configure(WrapperLookup pProvider) {
		getOrCreateTagBuilder(AllRecipeSerializerTags.AUTOMATION_IGNORE.tag).addOptional(Mods.OCCULTISM.rl("spirit_trade"))
		.addOptional(Mods.OCCULTISM.rl("ritual"));

		// VALIDATE

		for (AllRecipeSerializerTags tag : AllRecipeSerializerTags.values()) {
			if (tag.alwaysDatagen) {
				getTagBuilder(tag.tag);
			}
		}

	}

	@Override
	public String getName() {
		return "Create's Recipe Serializer Tags";
	}
}

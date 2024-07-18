package com.simibubi.create.content.kinetics.deployer;

import java.util.List;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.compat.recipeViewerCommon.SequencedAssemblySubCategoryType;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DeployerApplicationRecipe extends ItemApplicationRecipe implements IAssemblyRecipe {

	public DeployerApplicationRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.DEPLOYING, params);
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	public static DeployerApplicationRecipe convert(Recipe<?> sandpaperRecipe) {
		return new ProcessingRecipeBuilder<>(DeployerApplicationRecipe::new,
			new Identifier(sandpaperRecipe.getId()
				.getNamespace(),
				sandpaperRecipe.getId()
					.getPath() + "_using_deployer")).require(sandpaperRecipe.getIngredients()
						.get(0))
						.require(AllItemTags.SANDPAPER.tag)
						.output(sandpaperRecipe.getOutput(MinecraftClient.getInstance().world.getRegistryManager()))
						.build();
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {
		list.add(ingredients.get(1));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Text getDescriptionForAssembly() {
		ItemStack[] matchingStacks = ingredients.get(1)
			.getMatchingStacks();
		if (matchingStacks.length == 0)
			return Components.literal("Invalid");
		return Lang.translateDirect("recipe.assembly.deploying_item",
			Components.translatable(matchingStacks[0].getTranslationKey()).getString());
	}

	@Override
	public void addRequiredMachines(Set<ItemConvertible> list) {
		list.add(AllBlocks.DEPLOYER.get());
	}

	@Override
	public SequencedAssemblySubCategoryType getJEISubCategory() {
		return SequencedAssemblySubCategoryType.DEPLOYING;
	}

}

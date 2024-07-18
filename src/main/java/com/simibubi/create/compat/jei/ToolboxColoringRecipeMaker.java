package com.simibubi.create.compat.jei;

import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.TagValueAccessor;

public final class ToolboxColoringRecipeMaker {

	// From JEI's ShulkerBoxColoringRecipeMaker
	public static Stream<CraftingRecipe> createRecipes() {
		String group = "create.toolbox.color";
		ItemStack baseShulkerStack = AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.asStack();
		Ingredient baseShulkerIngredient = Ingredient.ofStacks(baseShulkerStack);

		return Arrays.stream(DyeColor.values())
			.filter(dc -> dc != DyeColor.BROWN)
			.map(color -> {
				DyeItem dye = DyeItem.byColor(color);
				ItemStack dyeStack = new ItemStack(dye);
				TagKey<Item> colorTag = color.getTag();
				Ingredient.Entry dyeList = new Ingredient.StackEntry(dyeStack);
				Ingredient.Entry colorList = TagValueAccessor.createTagValue(colorTag);
				Stream<Ingredient.Entry> colorIngredientStream = Stream.of(dyeList, colorList);
				Ingredient colorIngredient = Ingredient.ofEntries(colorIngredientStream);
				DefaultedList<Ingredient> inputs =
					DefaultedList.copyOf(Ingredient.EMPTY, baseShulkerIngredient, colorIngredient);
				Block coloredShulkerBox = AllBlocks.TOOLBOXES.get(color)
					.get();
				ItemStack output = new ItemStack(coloredShulkerBox);
				Identifier id = Create.asResource(group + "." + output.getTranslationKey());
				return new ShapelessRecipe(id, group, CraftingRecipeCategory.MISC, output, inputs);
			});
	}

	private ToolboxColoringRecipeMaker() {}

}

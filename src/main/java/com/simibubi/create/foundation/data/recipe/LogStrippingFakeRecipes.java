package com.simibubi.create.foundation.data.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipSection;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.AxeItemAccessor;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.simibubi.create.infrastructure.config.AllConfigs;

/**
 * Just in case players don't know about that vanilla feature
 */
public class LogStrippingFakeRecipes {

	public static List<ManualApplicationRecipe> createRecipes() {
		List<ManualApplicationRecipe> recipes = new ArrayList<>();
		if (!AllConfigs.server().recipes.displayLogStrippingRecipes.get())
			return recipes;

		ItemStack axe = new ItemStack(Items.IRON_AXE);
		axe.addHideFlag(TooltipSection.MODIFIERS);
		axe.setCustomName(Lang.translateDirect("recipe.item_application.any_axe")
			.styled(style -> style.withItalic(false)));
		// fabric: tag may not exist yet with JEI, #773
		Registries.ITEM.iterateEntries(ItemTags.LOGS)
			.forEach(stack -> process(stack.value(), recipes, axe));
		return recipes;
	}

	private static void process(Item item, List<ManualApplicationRecipe> list, ItemStack axe) {
		if (!(item instanceof BlockItem blockItem))
			return;
		BlockState state = blockItem.getBlock()
			.getDefaultState();
		BlockState strippedState = getStrippedState(state);
		if (strippedState == null)
			return;
		Item resultItem = strippedState.getBlock()
			.asItem();
		if (resultItem == null)
			return;
		list.add(create(item, resultItem, axe));
	}

	private static ManualApplicationRecipe create(Item fromItem, Item toItem, ItemStack axe) {
		Identifier rn = RegisteredObjects.getKeyOrThrow(toItem);
		return new ProcessingRecipeBuilder<>(ManualApplicationRecipe::new,
			new Identifier(rn.getNamespace(), rn.getPath() + "_via_vanilla_stripping")).require(fromItem)
				.require(Ingredient.ofStacks(axe))
				.output(toItem)
				.build();
	}

	@Nullable
	public static BlockState getStrippedState(BlockState state) {
		if (Items.IRON_AXE instanceof AxeItemAccessor axe) {
			return axe.porting_lib$getStripped(state).orElse(null);
		}
		return null;
	}
}

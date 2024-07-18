package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import io.github.fabricators_of_create.porting_lib.util.TagUtil;
import net.minecraft.block.Block;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ToolboxDyeingRecipe extends SpecialCraftingRecipe {

	public ToolboxDyeingRecipe(Identifier rl, CraftingRecipeCategory category) {
		super(rl, category);
	}

	@Override
	public boolean matches(RecipeInputInventory inventory, World world) {
		int toolboxes = 0;
		int dyes = 0;

		for (int i = 0; i < inventory.size(); ++i) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				if (Block.getBlockFromItem(stack.getItem()) instanceof ToolboxBlock) {
					++toolboxes;
				} else {
					if (!stack.isIn(Tags.Items.DYES))
						return false;
					++dyes;
				}

				if (dyes > 1 || toolboxes > 1) {
					return false;
				}
			}
		}

		return toolboxes == 1 && dyes == 1;
	}

	@Override
	public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager pRegistryAccess) {
		ItemStack toolbox = ItemStack.EMPTY;
		DyeColor color = DyeColor.BROWN;

		for (int i = 0; i < inventory.size(); ++i) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				if (Block.getBlockFromItem(stack.getItem()) instanceof ToolboxBlock) {
					toolbox = stack;
				} else {
					DyeColor color1 = TagUtil.getColorFromStack(stack);
					if (color1 != null) {
						color = color1;
					}
				}
			}
		}

		ItemStack dyedToolbox = AllBlocks.TOOLBOXES.get(color)
			.asStack();
		if (toolbox.hasNbt()) {
			dyedToolbox.setNbt(toolbox.getNbt()
				.copy());
		}

		return dyedToolbox;
	}

	@Override
	public boolean fits(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AllRecipeTypes.TOOLBOX_DYEING.getSerializer();
	}

}

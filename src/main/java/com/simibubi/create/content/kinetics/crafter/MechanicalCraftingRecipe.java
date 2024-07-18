package com.simibubi.create.content.kinetics.crafter;

import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class MechanicalCraftingRecipe extends ShapedRecipe {

	private boolean acceptMirrored;

	public MechanicalCraftingRecipe(Identifier idIn, String groupIn, int recipeWidthIn, int recipeHeightIn,
		DefaultedList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn, boolean acceptMirrored) {
		super(idIn, groupIn, CraftingRecipeCategory.MISC, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
		this.acceptMirrored = acceptMirrored;
	}

	private static MechanicalCraftingRecipe fromShaped(ShapedRecipe recipe, boolean acceptMirrored) {
		return new MechanicalCraftingRecipe(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(),
			recipe.getIngredients(), recipe.getOutput(null), acceptMirrored);
	}

	@Override
	public boolean matches(RecipeInputInventory inv, World worldIn) {
		if (!(inv instanceof MechanicalCraftingInventory))
			return false;
		if (acceptsMirrored())
			return super.matches(inv, worldIn);

		// From ShapedRecipe except the symmetry
		for (int i = 0; i <= inv.getWidth() - this.getWidth(); ++i)
			for (int j = 0; j <= inv.getHeight() - this.getHeight(); ++j)
				if (this.matchesSpecific(inv, i, j))
					return true;
		return false;
	}

	// From ShapedRecipe
	private boolean matchesSpecific(RecipeInputInventory inv, int p_77573_2_, int p_77573_3_) {
		DefaultedList<Ingredient> ingredients = getIngredients();
		int width = getWidth();
		int height = getHeight();
		for (int i = 0; i < inv.getWidth(); ++i) {
			for (int j = 0; j < inv.getHeight(); ++j) {
				int k = i - p_77573_2_;
				int l = j - p_77573_3_;
				Ingredient ingredient = Ingredient.EMPTY;
				if (k >= 0 && l >= 0 && k < width && l < height)
					ingredient = ingredients.get(k + l * width);
				if (!ingredient.test(inv.getStack(i + j * inv.getWidth())))
					return false;
			}
		}
		return true;
	}

	@Override
	public RecipeType<?> getType() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getType();
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AllRecipeTypes.MECHANICAL_CRAFTING.getSerializer();
	}

	public boolean acceptsMirrored() {
		return acceptMirrored;
	}

	public static class Serializer extends ShapedRecipe.Serializer {

		@Override
		public ShapedRecipe read(Identifier recipeId, JsonObject json) {
			return fromShaped(super.read(recipeId, json), JsonHelper.getBoolean(json, "acceptMirrored", true));
		}

		@Override
		public ShapedRecipe read(Identifier recipeId, PacketByteBuf buffer) {
			return fromShaped(super.read(recipeId, buffer), buffer.readBoolean() && buffer.readBoolean());
		}

		@Override
		public void write(PacketByteBuf p_199427_1_, ShapedRecipe p_199427_2_) {
			super.write(p_199427_1_, p_199427_2_);
			if (p_199427_2_ instanceof MechanicalCraftingRecipe) {
				p_199427_1_.writeBoolean(true);
				p_199427_1_.writeBoolean(((MechanicalCraftingRecipe) p_199427_2_).acceptsMirrored());
			} else
				p_199427_1_.writeBoolean(false);
		}

	}

}

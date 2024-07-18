package com.simibubi.create.compat.rei.category;

import java.util.List;
import com.simibubi.create.compat.rei.category.animations.AnimatedCrafter;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.shedaniel.math.FloatingDimension;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.collection.DefaultedList;

public class MechanicalCraftingCategory extends CreateRecipeCategory<CraftingRecipe> {

	private final AnimatedCrafter crafter = new AnimatedCrafter();

	public MechanicalCraftingCategory(Info<CraftingRecipe> info) {
		super(info);
	}

	static int maxSize = 100;

	public static float getScale(CraftingRecipe recipe) {
		int w = getWidth(recipe);
		int h = getHeight(recipe);
		return Math.min(1, maxSize / (19f * Math.max(w, h)));
	}

	public static int getYPadding(CraftingRecipe recipe) {
		return 3 + 50 - (int) (getScale(recipe) * getHeight(recipe) * 19 * .5);
	}

	public static int getXPadding(CraftingRecipe recipe) {
		return 3 + 50 - (int) (getScale(recipe) * getWidth(recipe) * 19 * .5);
	}

	private static int getWidth(CraftingRecipe recipe) {
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe)recipe).getWidth() : 1;
	}

	private static int getHeight(CraftingRecipe recipe) {
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe)recipe).getHeight() : 1;
	}

	@Override
	public List<Widget> setupDisplay(CreateDisplay<CraftingRecipe> display, Rectangle pos) {
		List<Widget> widgets = super.setupDisplay(display, pos);
		CraftingRecipe recipe = display.getRecipe();

		Slot result = Widgets.createSlot(new Point( pos.x + 134, pos.y + 81))
				.entries(List.of(EntryStacks.of(getResultItem(recipe))))
				.disableBackground();
		widgets.add(result);

		int x = getXPadding(recipe);
		int y = getYPadding(recipe);
		float scale = getScale(recipe);
		FloatingDimension slotSize = new FloatingDimension(18 * scale, 18 * scale);

		DefaultedList<Ingredient> ingredients = recipe.getIngredients();
		for (int i = 0; i < ingredients.size(); i++) {
			float f = 19 * scale;
			int xPosition = (int) (x + 1 + (i % getWidth(recipe)) * f) + pos.x;
			int yPosition = (int) (y + 1 + (i / getWidth(recipe)) * f) + pos.y;

			EntryIngredient ingredient = EntryIngredients.ofIngredient(ingredients.get(i));
			if (ingredient.isEmpty())
				continue;
			Slot slot = Widgets.createSlot(new Point(xPosition, yPosition))
					.disableBackground().markInput().entries(ingredient);
			slot.getBounds().setSize(slotSize);
			widgets.add(slot);
		}

		return widgets;
	}

	@Override
	public void draw(CraftingRecipe recipe, DrawContext graphics, double mouseX, double mouseY) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		matrixStack.translate(0, -4, 0); // why?
		matrixStack.push();
		float scale = getScale(recipe);
		matrixStack.translate(getXPadding(recipe), getYPadding(recipe), 0);

		for (int i = 0; i < recipe.getIngredients().size(); i++)
			if (!recipe.getIngredients().get(i).isEmpty()) {
				int row = i / getWidth(recipe);
				int col = i % getWidth(recipe);

				matrixStack.push();
				matrixStack.translate(col * 19 * scale, row * 19 * scale, 0);
				matrixStack.scale(scale, scale, scale);
				AllGuiTextures.JEI_SLOT.render(graphics, 0, 0);
				matrixStack.pop();
			}

		matrixStack.pop();

		AllGuiTextures.JEI_SLOT.render(graphics, 133, 80);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 128, 59);
		crafter.draw(graphics, 129, 25);

		matrixStack.push();
		matrixStack.translate(0, 0, 300);

		int amount = 0;
		for (Ingredient ingredient : recipe.getIngredients()) {
			if (Ingredient.EMPTY == ingredient)
				continue;
			amount++;
		}

		graphics.drawText(MinecraftClient.getInstance().textRenderer, String.valueOf(amount), 142, 39, 0xFFFFFF, true);
		matrixStack.pop();
		matrixStack.pop();
	}
}

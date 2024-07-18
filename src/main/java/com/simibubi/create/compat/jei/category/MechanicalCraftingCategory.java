package com.simibubi.create.compat.jei.category;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.compat.jei.category.animations.AnimatedCrafter;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Components;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ParametersAreNonnullByDefault
public class MechanicalCraftingCategory extends CreateRecipeCategory<CraftingRecipe> {

	private final AnimatedCrafter crafter = new AnimatedCrafter();

	public MechanicalCraftingCategory(Info<CraftingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CraftingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 81)
			.addItemStack(getResultItem(recipe));

		int x = getXPadding(recipe);
		int y = getYPadding(recipe);
		float scale = getScale(recipe);

		IIngredientRenderer<ItemStack> renderer = new CrafterIngredientRenderer(recipe);
		int i = 0;

		for (Ingredient ingredient : recipe.getIngredients()) {
			float f = 19 * scale;
			int xPosition = (int) (x + 1 + (i % getWidth(recipe)) * f);
			int yPosition = (int) (y + 1 + (i / getWidth(recipe)) * f);

			builder.addSlot(RecipeIngredientRole.INPUT, xPosition, yPosition)
				.setCustomRenderer(VanillaTypes.ITEM_STACK, renderer)
				.addIngredients(ingredient);

			i++;
		}

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
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getWidth() : 1;
	}

	private static int getHeight(CraftingRecipe recipe) {
		return recipe instanceof ShapedRecipe ? ((ShapedRecipe) recipe).getHeight() : 1;
	}

	@Override
	public void draw(CraftingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, DrawContext graphics, double mouseX,
		double mouseY) {
		MatrixStack matrixStack = graphics.getMatrices();
		matrixStack.push();
		float scale = getScale(recipe);
		matrixStack.translate(getXPadding(recipe), getYPadding(recipe), 0);

		for (int row = 0; row < getHeight(recipe); row++)
			for (int col = 0; col < getWidth(recipe); col++)
				if (!recipe.getIngredients()
					.get(row * getWidth(recipe) + col)
					.isEmpty()) {
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

		graphics.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, amount + "", 142, 39, 0xFFFFFF);
		matrixStack.pop();
	}

	private static final class CrafterIngredientRenderer implements IIngredientRenderer<ItemStack> {

		private final CraftingRecipe recipe;
		private final float scale;

		public CrafterIngredientRenderer(CraftingRecipe recipe) {
			this.recipe = recipe;
			scale = getScale(recipe);
		}

		@Override
		public void render(DrawContext graphics, @NotNull ItemStack ingredient) {
			MatrixStack matrixStack = graphics.getMatrices();
			matrixStack.push();
			float scale = getScale(recipe);
			matrixStack.scale(scale, scale, scale);

			if (ingredient != null) {
				MatrixStack modelViewStack = RenderSystem.getModelViewStack();
				modelViewStack.push();
				RenderSystem.applyModelViewMatrix();
				RenderSystem.enableDepthTest();
				MinecraftClient minecraft = MinecraftClient.getInstance();
				TextRenderer font = getFontRenderer(minecraft, ingredient);
				graphics.drawItem(ingredient, 0, 0);
				graphics.drawItemInSlot(font, ingredient, 0, 0, null);
				RenderSystem.disableBlend();
				modelViewStack.pop();
				RenderSystem.applyModelViewMatrix();
			}

			matrixStack.pop();
		}

		@Override
		public int getWidth() {
			return (int) (16 * scale);
		}

		@Override
		public int getHeight() {
			return (int) (16 * scale);
		}

		@Override
		public List<Text> getTooltip(ItemStack ingredient, TooltipContext tooltipFlag) {
			MinecraftClient minecraft = MinecraftClient.getInstance();
			PlayerEntity player = minecraft.player;
			try {
				return ingredient.getTooltip(player, tooltipFlag);
			} catch (RuntimeException | LinkageError e) {
				List<Text> list = new ArrayList<>();
				MutableText crash = Components.translatable("jei.tooltip.error.crash");
				list.add(crash.formatted(Formatting.RED));
				return list;
			}
		}
	}

}

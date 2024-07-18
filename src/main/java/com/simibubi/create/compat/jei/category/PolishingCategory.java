package com.simibubi.create.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.GuiGameElement;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.collection.DefaultedList;

@ParametersAreNonnullByDefault
public class PolishingCategory extends CreateRecipeCategory<SandPaperPolishingRecipe> {

	private final ItemStack renderedSandpaper;

	public PolishingCategory(Info<SandPaperPolishingRecipe> info) {
		super(info);
		renderedSandpaper = AllItems.SAND_PAPER.asStack();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SandPaperPolishingRecipe recipe, IFocusGroup focuses) {
		builder
				.addSlot(RecipeIngredientRole.INPUT, 27, 29)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(recipe.getIngredients().get(0));

		ProcessingOutput output = recipe.getRollableResults().get(0);
		builder
				.addSlot(RecipeIngredientRole.OUTPUT, 132, 29)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addTooltipCallback(addStochasticTooltip(output));
	}

	@Override
	public void draw(SandPaperPolishingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, DrawContext graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 61, 21);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 32);

		DefaultedList<Ingredient> ingredients = recipe.getIngredients();
		ItemStack[] matchingStacks = ingredients.get(0)
			.getMatchingStacks();
		if (matchingStacks.length == 0)
			return;


		NbtCompound tag = renderedSandpaper.getOrCreateNbt();
		tag.put("Polishing", NBTSerializer.serializeNBTCompound(matchingStacks[0]));
		tag.putBoolean("JEI", true);
		GuiGameElement.of(renderedSandpaper)
				.<GuiGameElement.GuiRenderBuilder>at(getBackground().getWidth() / 2 - 16, 0, 0)
				.scale(2)
				.render(graphics);
	}

}

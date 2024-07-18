package com.simibubi.create.compat.jei.category;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.math.RotationAxis;

@ParametersAreNonnullByDefault
public abstract class ProcessingViaFanCategory<T extends Recipe<?>> extends CreateRecipeCategory<T> {

	protected static final int SCALE = 24;

	public ProcessingViaFanCategory(Info<T> info) {
		super(info);
	}

	public static Supplier<ItemStack> getFan(String name) {
		return () -> AllBlocks.ENCASED_FAN.asStack()
			.setCustomName(Lang.translateDirect("recipe." + name + ".fan").styled(style -> style.withItalic(false)));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
		builder
				.addSlot(RecipeIngredientRole.INPUT, 21, 48)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(recipe.getIngredients().get(0));
		builder
				.addSlot(RecipeIngredientRole.OUTPUT, 141, 48)
				.setBackground(getRenderedSlot(), -1, -1)
				.addItemStack(getResultItem(recipe));
	}

	@Override
	public void draw(T recipe, IRecipeSlotsView iRecipeSlotsView, DrawContext graphics, double mouseX, double mouseY) {
		renderWidgets(graphics, recipe, mouseX, mouseY);
		
		MatrixStack matrixStack = graphics.getMatrices();

		matrixStack.push();
		translateFan(matrixStack);
		matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-12.5f));
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(22.5f));

		AnimatedKinetics.defaultBlockElement(AllPartialModels.ENCASED_FAN_INNER)
			.rotateBlock(180, 0, AnimatedKinetics.getCurrentAngle() * 16)
			.scale(SCALE)
			.render(graphics);

		AnimatedKinetics.defaultBlockElement(AllBlocks.ENCASED_FAN.getDefaultState())
			.rotateBlock(0, 180, 0)
			.atLocal(0, 0, 0)
			.scale(SCALE)
			.render(graphics);

		renderAttachedBlock(graphics);
		matrixStack.pop();
	}

	protected void renderWidgets(DrawContext graphics, T recipe, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
		getBlockShadow().render(graphics, 65, 39);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 54, 51);
	}

	protected AllGuiTextures getBlockShadow() {
		return AllGuiTextures.JEI_SHADOW;
	}

	protected void translateFan(MatrixStack matrixStack) {
		matrixStack.translate(56, 33, 0);
	}

	protected abstract void renderAttachedBlock(DrawContext graphics);

	public static abstract class MultiOutput<T extends ProcessingRecipe<?>> extends ProcessingViaFanCategory<T> {

		public MultiOutput(Info<T> info) {
			super(info);
		}

		@Override
		public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
			List<ProcessingOutput> results = recipe.getRollableResults();
			int xOffsetAmount = 1 - Math.min(3, results.size());

			builder
					.addSlot(RecipeIngredientRole.INPUT, 5 * xOffsetAmount + 21, 48)
					.setBackground(getRenderedSlot(), -1, -1)
					.addIngredients(recipe.getIngredients().get(0));

			int i = 0;
			boolean excessive = results.size() > 9;
			for (ProcessingOutput output : results) {
				int xOffset = (i % 3) * 19 + 9 * xOffsetAmount;
				int yOffset = (i / 3) * -19 + (excessive ? 8 : 0);

				builder
						.addSlot(RecipeIngredientRole.OUTPUT, 141 + xOffset, 48 + yOffset)
						.setBackground(getRenderedSlot(output), -1, -1)
						.addItemStack(output.getStack())
						.addTooltipCallback(addStochasticTooltip(output));
				i++;
			}
		}

		@Override
		protected void renderWidgets(DrawContext graphics, T recipe, double mouseX, double mouseY) {
			int size = recipe.getRollableResultsAsItemStacks().size();
			int xOffsetAmount = 1 - Math.min(3, size);

			AllGuiTextures.JEI_SHADOW.render(graphics, 46, 29);
			getBlockShadow().render(graphics, 65, 39);
			AllGuiTextures.JEI_LONG_ARROW.render(graphics, 7 * xOffsetAmount + 54, 51);

		}

	}

}

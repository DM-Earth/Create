package com.simibubi.create.compat.rei.category.sequencedAssembly;

import static com.simibubi.create.compat.rei.category.CreateRecipeCategory.basicSlot;
import static com.simibubi.create.compat.rei.category.CreateRecipeCategory.setFluidTooltip;

import java.util.Arrays;
import java.util.List;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.rei.category.animations.AnimatedPress;
import com.simibubi.create.compat.rei.category.animations.AnimatedSaw;
import com.simibubi.create.compat.rei.category.animations.AnimatedSpout;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;

public abstract class ReiSequencedAssemblySubCategory {

	private int width;

	public ReiSequencedAssemblySubCategory(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public int addItemIngredients(SequencedRecipe<?> recipe, List<Widget> widgets, int x, int index, Point origin) {
		return 0;
	}

	public int addFluidIngredients(SequencedRecipe<?> recipe, List<Widget> widgets, int x, int index, Point origin) {
		return 0;
	}

	public abstract void draw(SequencedRecipe<?> recipe, DrawContext graphics, double mouseX, double mouseY, int index);

	public static class AssemblyPressing extends ReiSequencedAssemblySubCategory {

		AnimatedPress press;

		public AssemblyPressing() {
			super(25);
			press = new AnimatedPress(false);
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, DrawContext graphics, double mouseX, double mouseY, int index) {
			MatrixStack ms = graphics.getMatrices();
			press.offset = index;
			ms.push();
			ms.translate(-5, 50, 0);
			ms.scale(.6f, .6f, .6f);
			press.draw(graphics, getWidth() / 2, 0);
			ms.pop();
		}

	}

	public static class AssemblySpouting extends ReiSequencedAssemblySubCategory {

		AnimatedSpout spout;

		public AssemblySpouting() {
			super(25);
			spout = new AnimatedSpout();
		}

		@Override
		public int addFluidIngredients(SequencedRecipe<?> recipe, List<Widget> widgets, int x, int index, Point origin) {
			FluidIngredient fluidIngredient = recipe.getRecipe()
				.getFluidIngredients()
				.get(0);
			// Always pass 0 to the fluid ingredient get, because spouting only supports one fluid per step
			// and the passed index to this method will produce an out-of-bounds exception if used
			Slot fluidSlot = basicSlot(x + 4, 15, origin).markInput().entries(EntryIngredients.of(CreateRecipeCategory.convertToREIFluid(fluidIngredient.getMatchingFluidStacks().get(0))));
			CreateRecipeCategory.setFluidRenderRatio(fluidSlot);
			setFluidTooltip(fluidSlot);
			widgets.add(fluidSlot);
			return 1;
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, DrawContext graphics, double mouseX, double mouseY, int index) {
			MatrixStack ms = graphics.getMatrices();
			spout.offset = index;
			AllGuiTextures.JEI_SLOT.render(graphics, 3, 14);
			ms.push();
			ms.translate(-7, 50, 0);
			ms.scale(.75f, .75f, .75f);
			spout.withFluids(recipe.getRecipe()
				.getFluidIngredients()
				.get(0)
				.getMatchingFluidStacks())
				.draw(graphics, getWidth() / 2, 0);
			ms.pop();
		}

	}

	public static class AssemblyDeploying extends ReiSequencedAssemblySubCategory {

		AnimatedDeployer deployer;

		public AssemblyDeploying() {
			super(25);
			deployer = new AnimatedDeployer();
		}

		@Override
		public int addItemIngredients(SequencedRecipe<?> recipe, List<Widget> widgets, int x, int index, Point origin) {
			EntryIngredient entryIngredient = EntryIngredients.ofItemStacks(Arrays.asList(recipe.getRecipe()
					.getIngredients()
					.get(1)
					.getMatchingStacks()));
			entryIngredient.forEach(entryStack -> {
				IAssemblyRecipe contained = recipe.getAsAssemblyRecipe();
				if (contained instanceof DeployerApplicationRecipe && ((DeployerApplicationRecipe) contained).shouldKeepHeldItem()) {
					entryStack.tooltip(Lang.translateDirect("recipe.deploying.not_consumed")
							.formatted(Formatting.GOLD));
				}
			});
			widgets.add(basicSlot(origin.x + x + 4, origin.y + 15)
					.markInput()
					.entries(entryIngredient));

			return 1;
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, DrawContext graphics, double mouseX, double mouseY, int index) {
			MatrixStack ms = graphics.getMatrices();
			deployer.offset = index;
			ms.push();
			ms.translate(-7, 50, 0);
			ms.scale(.75f, .75f, .75f);
			deployer.draw(graphics, getWidth() / 2, 0);
			ms.pop();
			AllGuiTextures.JEI_SLOT.render(graphics, 3, 14);
		}

	}

	public static class AssemblyCutting extends ReiSequencedAssemblySubCategory {

		AnimatedSaw saw;

		public AssemblyCutting() {
			super(25);
			saw = new AnimatedSaw();
		}

		@Override
		public void draw(SequencedRecipe<?> recipe, DrawContext graphics, double mouseX, double mouseY, int index) {
			MatrixStack ms = graphics.getMatrices();
			ms.push();
			ms.translate(0, 51.5f, 0);
			ms.scale(.6f, .6f, .6f);
			saw.draw(graphics, getWidth() / 2, 30);
			ms.pop();
		}

	}

}

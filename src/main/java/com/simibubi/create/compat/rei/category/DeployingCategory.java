package com.simibubi.create.compat.rei.category;

import java.util.List;
import com.simibubi.create.compat.rei.category.animations.AnimatedDeployer;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

public class DeployingCategory extends CreateRecipeCategory<DeployerApplicationRecipe> {

	private final AnimatedDeployer deployer = new AnimatedDeployer();

	public DeployingCategory(Info<DeployerApplicationRecipe> info) {
		super(info);
	}

	@Override
	public void addWidgets(CreateDisplay<DeployerApplicationRecipe> display, List<Widget> ingredients, Point origin) {
		DeployerApplicationRecipe recipe = display.getRecipe();
		Slot input = basicSlot(origin.getX() + 27, origin.getY() + 51)
				.markInput()
				.entries(EntryIngredients.ofIngredient(recipe.getProcessedItem()));
		ClientEntryStacks.setTooltipProcessor(input.getCurrentEntry(), ((entryStack, tooltip) -> {
			if (recipe.shouldKeepHeldItem())
					tooltip.add(Lang.translateDirect("recipe.deploying.not_consumed")
							.formatted(Formatting.GOLD));
			return tooltip;
		}));
		ingredients.add(input);
		ingredients.add(basicSlot(origin.getX() + 51, origin.getY() + 5)
				.markInput()
				.entries(EntryIngredients.ofIngredient(recipe.getRequiredHeldItem())));
		recipe.getRollableResults().stream().limit(1).forEach(result -> {
			Slot outputSlot = basicSlot(origin.getX() + 132, origin.getY() + 51)
					.markOutput()
					.entries(EntryIngredients.of(result.getStack()));
			ingredients.add(outputSlot);
			addStochasticTooltip(outputSlot, result);
		});
	}

	@Override
	public void draw(DeployerApplicationRecipe recipe, DrawContext graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SLOT.render(graphics, 50, 4);
		AllGuiTextures.JEI_SLOT.render(graphics, 26, 50);
		getRenderedSlot(recipe, 0).render(graphics, 131, 50);
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
		deployer.draw(graphics, getDisplayWidth(null) / 2 - 13, 22);
	}

}

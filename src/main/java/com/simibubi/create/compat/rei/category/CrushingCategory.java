package com.simibubi.create.compat.rei.category;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.compat.rei.category.animations.AnimatedCrushingWheels;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.Lang;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CrushingCategory extends CreateRecipeCategory<AbstractCrushingRecipe> {

	public CrushingCategory(Info<AbstractCrushingRecipe> info) {
		super(info);
	}

	@Override
	public List<Widget> setupDisplay(CreateDisplay<AbstractCrushingRecipe> display, Rectangle bounds) {
		Point origin = new Point(bounds.getX(), bounds.getY() + 4);
		List<Widget> widgets = new ArrayList<>();
		List<ProcessingOutput> results = display.getRecipe().getRollableResults();
		widgets.add(Widgets.createRecipeBase(bounds));
		widgets.add(Widgets.createSlot(new Point(origin.x + 51, origin.y + 3)).disableBackground().markInput().entries(display.getInputEntries().get(0)));
		widgets.add(WidgetUtil.textured(AllGuiTextures.JEI_SLOT, origin.x + 50, origin.y + 2));
		widgets.add(WidgetUtil.textured(AllGuiTextures.JEI_DOWN_ARROW, origin.x + 72, origin.y + 7));

		int size = results.size();
		int offset = -size * 19 / 2;
		for (int outputIndex = 0; outputIndex < results.size(); outputIndex++) {
			List<Text> tooltip = new ArrayList<>();
			if (results.get(outputIndex).getChance() != 1)
				tooltip.add(Lang.translateDirect("recipe.processing.chance", results.get(outputIndex).getChance() < 0.01 ? "<1" : (int) (results.get(outputIndex).getChance() * 100))
								.formatted(Formatting.GOLD));
			widgets.add(Widgets.createSlot(new Point((origin.x + getDisplayWidth(display) / 2 + offset + 19 * outputIndex) + 1, origin.y + 78 + 1)).disableBackground().markOutput().entry(EntryStack.of(VanillaEntryTypes.ITEM, results.get(outputIndex).getStack()).tooltip(tooltip)));
			widgets.add(WidgetUtil.textured(getRenderedSlot(display.getRecipe(), outputIndex), origin.x + getDisplayWidth(display) / 2 + offset + 19 * outputIndex, origin.y + 78));
		}
		AnimatedCrushingWheels crushingWheels = new AnimatedCrushingWheels();
		crushingWheels.setPos(new Point(origin.getX() + 62, origin.getY() + 59));
		widgets.add(crushingWheels);
		return widgets;
	}

}

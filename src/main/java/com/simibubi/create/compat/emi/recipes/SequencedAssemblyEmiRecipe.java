package com.simibubi.create.compat.emi.recipes;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import com.simibubi.create.compat.emi.CreateEmiPlugin;
import com.simibubi.create.compat.emi.EmiSequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;

public class SequencedAssemblyEmiRecipe extends CreateEmiRecipe<SequencedAssemblyRecipe> {
	public static final String[] ROMAN = {
		"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX",
		"X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX",
		"XX", "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII", "XXIX", "-" };
	private final int margin = 3;
	private int width;

	public SequencedAssemblyEmiRecipe(SequencedAssemblyRecipe recipe) {
		super(CreateEmiPlugin.SEQUENCED_ASSEMBLY, recipe, 180, 120);
		for (SequencedRecipe<?> r : recipe.getSequence()) {
			width += getSubCategory(r).getWidth() + margin;
		}
		width -= margin;
	}

	@Override
	public int getDisplayWidth() {
		return Math.max(150, width);
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		int xOff = recipe.getOutputChance() == 1 ? 0 : -7;
		int mid = widgets.getWidth() / 2;

		addTexture(widgets, AllGuiTextures.JEI_LONG_ARROW, mid - 38 + xOff, 94);

		widgets.addDrawable(mid - 38 + xOff, 94, AllGuiTextures.JEI_LONG_ARROW.width, 20, (matrices, mx, my, delta) -> {})
			.tooltip((mouseX, mouseY) -> List.of(TooltipComponent.of(
				Lang.translateDirect("recipe.assembly.repeat", recipe.getLoops()).asOrderedText())));

		if (recipe.getOutputChance() != 1) {
			float chance = recipe.getOutputChance();
			addTexture(widgets, AllGuiTextures.JEI_CHANCE_SLOT, mid + 60 + xOff, 90)
				.tooltip((mouseX, mouseY) -> List.of(
					TooltipComponent.of(Lang.translateDirect("recipe.assembly.junk").asOrderedText()),
					TooltipComponent.of(Components.translatable("tooltip.emi.chance.produce", chance > 0.99 ? "<1" : 100 - (int) (chance * 100))
						.formatted(Formatting.GOLD).asOrderedText())
				));
		}

		addSlot(widgets, EmiIngredient.of(recipe.getIngredient()), mid - 64 + xOff, 90);

		addSlot(widgets, output.get(0), mid + 41 + xOff, 90).recipeContext(this);

		int sx = width / -2 + mid;
		int x = sx;
		int index = 0;
		for (SequencedRecipe<?> recipe : recipe.getSequence()) {
			EmiSequencedAssemblySubCategory category = getSubCategory(recipe);
			category.addWidgets(widgets, x, 0, recipe, index++);
			x += category.getWidth() + margin;
		}

		widgets.addDrawable(0, 0, 0, 0, (graphics, mouseX, mouseY, delta) -> {
			MatrixStack matrices = graphics.getMatrices();
			MinecraftClient client = MinecraftClient.getInstance();
			matrices.push();
			matrices.translate(0, 15, 0);
			if (recipe.getOutputChance() != 1) {
				graphics.drawText(client.textRenderer, "?", mid + 69 + xOff - client.textRenderer.getWidth("?") / 2, 80, 0xefefef, true);
			}

			if (recipe.getLoops() > 1) {
				matrices.push();
				matrices.translate(15, 9, 0);
				AllIcons.I_SEQ_REPEAT.render(graphics, mid - 40 + xOff, 75);
				graphics.drawText(client.textRenderer, "x" + recipe.getLoops(), mid - 24 + xOff, 80, 0x888888, true);
				matrices.pop();
			}
			matrices.pop();

			int cx = sx;
			for (int i = 0; i < recipe.getSequence().size(); i++) {
				String text = ROMAN[Math.min(i, ROMAN.length)];
				int w = getSubCategory(recipe.getSequence().get(i)).getWidth();
				int off = w / 2 - client.textRenderer.getWidth(text) / 2;
				graphics.drawText(client.textRenderer, text, cx + off, 2, 0x888888, true);
				cx += w + margin;
			}
		});
	}

	public static EmiSequencedAssemblySubCategory getSubCategory(SequencedRecipe<?> recipe) {
		return recipe.getAsAssemblyRecipe()
				.getJEISubCategory()
				.emi()
				.get()
				.get();
	}
}

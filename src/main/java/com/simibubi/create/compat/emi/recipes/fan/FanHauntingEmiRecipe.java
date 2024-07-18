package com.simibubi.create.compat.emi.recipes.fan;

import com.simibubi.create.compat.emi.CreateEmiAnimations;
import com.simibubi.create.compat.emi.CreateEmiPlugin;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;

public class FanHauntingEmiRecipe extends FanEmiRecipe.MultiOutput<HauntingRecipe> {

	public FanHauntingEmiRecipe(HauntingRecipe recipe) {
		super(CreateEmiPlugin.FAN_HAUNTING, recipe);
	}

	@Override
	protected void renderAttachedBlock(DrawContext graphics) {
		GuiGameElement.of(Blocks.SOUL_FIRE.getDefaultState())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(CreateEmiAnimations.DEFAULT_LIGHTING)
			.render(graphics);
	}
}

package com.simibubi.create.compat.emi.recipes.fan;

import com.simibubi.create.compat.emi.CreateEmiAnimations;
import com.simibubi.create.compat.emi.CreateEmiPlugin;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.recipe.SmokingRecipe;
import net.minecraft.util.Identifier;

public class FanSmokingEmiRecipe extends FanEmiRecipe<SmokingRecipe> {

	public FanSmokingEmiRecipe(SmokingRecipe recipe) {
		super(CreateEmiPlugin.FAN_SMOKING, recipe);
		Identifier rid = recipe.getId();
		this.id = new Identifier("emi", "create/fan_smoking/" + rid.getNamespace() + "/" + rid.getPath());
	}

	@Override
	protected void renderAttachedBlock(DrawContext graphics) {
		GuiGameElement.of(Blocks.FIRE.getDefaultState())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(CreateEmiAnimations.DEFAULT_LIGHTING)
			.render(graphics);
	}
}

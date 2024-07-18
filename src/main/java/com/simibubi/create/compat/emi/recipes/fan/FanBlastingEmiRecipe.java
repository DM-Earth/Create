package com.simibubi.create.compat.emi.recipes.fan;

import com.simibubi.create.compat.emi.CreateEmiAnimations;
import com.simibubi.create.compat.emi.CreateEmiPlugin;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.util.Identifier;

public class FanBlastingEmiRecipe extends FanEmiRecipe<AbstractCookingRecipe> {

	public FanBlastingEmiRecipe(AbstractCookingRecipe recipe) {
		super(CreateEmiPlugin.FAN_BLASTING, recipe);
		Identifier rid = recipe.getId();
		this.id = new Identifier("emi", "create/fan_blasting/" + rid.getNamespace() + "/" + rid.getPath());
	}

	@Override
	protected void renderAttachedBlock(DrawContext graphics) {
		GuiGameElement.of(Fluids.LAVA)
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(CreateEmiAnimations.DEFAULT_LIGHTING)
			.render(graphics);
	}
}

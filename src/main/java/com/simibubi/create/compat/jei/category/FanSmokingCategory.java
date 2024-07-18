package com.simibubi.create.compat.jei.category;

import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.recipe.SmokingRecipe;

public class FanSmokingCategory extends ProcessingViaFanCategory<SmokingRecipe> {

	public FanSmokingCategory(Info<SmokingRecipe> info) {
		super(info);
	}

	@Override
	protected AllGuiTextures getBlockShadow() {
		return AllGuiTextures.JEI_LIGHT;
	}

	@Override
	protected void renderAttachedBlock(DrawContext graphics) {
		GuiGameElement.of(Blocks.FIRE.getDefaultState())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}

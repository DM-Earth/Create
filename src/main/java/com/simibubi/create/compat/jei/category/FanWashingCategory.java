package com.simibubi.create.compat.jei.category;

import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.fluid.Fluids;

public class FanWashingCategory extends ProcessingViaFanCategory.MultiOutput<SplashingRecipe> {

	public FanWashingCategory(Info<SplashingRecipe> info) {
		super(info);
	}

	@Override
	protected void renderAttachedBlock(DrawContext graphics) {
		GuiGameElement.of(Fluids.WATER)
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}

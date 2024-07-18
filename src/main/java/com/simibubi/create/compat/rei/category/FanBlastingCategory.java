package com.simibubi.create.compat.rei.category;

import org.jetbrains.annotations.NotNull;
import com.simibubi.create.compat.rei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.fluid.Fluids;
import net.minecraft.recipe.AbstractCookingRecipe;

public class FanBlastingCategory extends ProcessingViaFanCategory<AbstractCookingRecipe> {

	public FanBlastingCategory(Info<AbstractCookingRecipe> info) {
		super(info);
	}

	@Override
	protected AllGuiTextures getBlockShadow() {
		return AllGuiTextures.JEI_LIGHT;
	}

	@Override
	protected void renderAttachedBlock(@NotNull DrawContext graphics) {
		GuiGameElement.of(Fluids.LAVA)
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}

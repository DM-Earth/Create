package com.simibubi.create.content.kinetics.crusher;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.World;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;

@ParametersAreNonnullByDefault
public class CrushingRecipe extends AbstractCrushingRecipe {

	public CrushingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.CRUSHING, params);
	}

	@Override
	public boolean matches(Inventory inv, World worldIn) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0)
			.test(inv.getStack(0));
	}

	@Override
	protected int getMaxOutputCount() {
		return 7;
	}

}

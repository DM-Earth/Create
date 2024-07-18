package com.simibubi.create.compat.jei;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import mezz.jei.api.constants.RecipeTypes;

import mezz.jei.api.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.equipment.blueprint.BlueprintAssignCompleteRecipePacket;
import com.simibubi.create.content.equipment.blueprint.BlueprintMenu;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlueprintTransferHandler implements IRecipeTransferHandler<BlueprintMenu, CraftingRecipe> {

	@Override
	public Class<BlueprintMenu> getContainerClass() {
		return BlueprintMenu.class;
	}

	@Override
	public Optional<ScreenHandlerType<BlueprintMenu>> getMenuType() {
		return Optional.empty();
	}

	@Override
	public RecipeType<CraftingRecipe> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(BlueprintMenu menu, CraftingRecipe craftingRecipe, IRecipeSlotsView recipeSlots, PlayerEntity player, boolean maxTransfer, boolean doTransfer) {
		if (!doTransfer)
			return null;

		AllPackets.getChannel().sendToServer(new BlueprintAssignCompleteRecipePacket(craftingRecipe.getId()));
		return null;
	}

}

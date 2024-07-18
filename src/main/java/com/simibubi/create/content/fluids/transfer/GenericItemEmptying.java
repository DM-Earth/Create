package com.simibubi.create.content.fluids.transfer;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.MutableContainerItemContext;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerContainer;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.recipe.Recipe;
import net.minecraft.world.World;

public class GenericItemEmptying {

	private static final Inventory WRAPPER = new ItemStackHandlerContainer(1);

	public static boolean canItemBeEmptied(World world, ItemStack stack) {
		if (stack.getItem() instanceof PotionItem)
			return true;

		WRAPPER.setStack(0, stack);
		if (AllRecipeTypes.EMPTYING.find(WRAPPER, world)
			.isPresent())
			return true;

		return TransferUtil.getFluidContained(stack).isPresent();
	}

	public static Pair<FluidStack, ItemStack> emptyItem(World world, ItemStack stack, boolean simulate) {
		FluidStack resultingFluid = FluidStack.EMPTY;
		ItemStack resultingItem = ItemStack.EMPTY;

		if (stack.getItem() instanceof PotionItem)
			return PotionFluidHandler.emptyPotion(stack, simulate);

		WRAPPER.setStack(0, stack);
		Optional<Recipe<Inventory>> recipe = AllRecipeTypes.EMPTYING.find(WRAPPER, world);
		if (recipe.isPresent()) {
			EmptyingRecipe emptyingRecipe = (EmptyingRecipe) recipe.get();
			List<ItemStack> results = emptyingRecipe.rollResults();
			if (!simulate)
				stack.decrement(1);
			resultingItem = results.isEmpty() ? ItemStack.EMPTY : results.get(0);
			resultingFluid = emptyingRecipe.getResultingFluid();
			return Pair.of(resultingFluid, resultingItem);
		}

		ItemStack split = stack.copy();
		split.setCount(1);
		MutableContainerItemContext ctx = new MutableContainerItemContext(split);
		Storage<FluidVariant> tank = FluidStorage.ITEM.find(split, ctx);
		if (tank == null)
			return Pair.of(resultingFluid, resultingItem);
		try (Transaction t = TransferUtil.getTransaction()) {
			resultingFluid = TransferUtil.extractAnyFluid(tank, FluidConstants.BUCKET);
			resultingItem = ctx.getItemVariant().toStack((int) ctx.getAmount());
			if (!simulate) {
				stack.decrement(1);
				t.commit();
			}

			return Pair.of(resultingFluid, resultingItem);
		}
	}

}

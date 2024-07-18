package com.simibubi.create.foundation.recipe;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.world.World;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.item.ItemHelper;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;

public class RecipeApplier {
	public static void applyRecipeOn(ItemEntity entity, Recipe<?> recipe) {
		List<ItemStack> stacks = applyRecipeOn(entity.getWorld(), entity.getStack(), recipe);
		if (stacks == null)
			return;
		if (stacks.isEmpty()) {
			entity.discard();
			return;
		}
		entity.setStack(stacks.remove(0));
		for (ItemStack additional : stacks) {
			ItemEntity entityIn = new ItemEntity(entity.getWorld(), entity.getX(), entity.getY(), entity.getZ(), additional);
			entityIn.setVelocity(entity.getVelocity());
			entity.getWorld().spawnEntity(entityIn);
		}
	}

	public static List<ItemStack> applyRecipeOn(World level, ItemStack stackIn, Recipe<?> recipe) {
		List<ItemStack> stacks;

		if (recipe instanceof ProcessingRecipe<?> pr) {
			stacks = new ArrayList<>();
			for (int i = 0; i < stackIn.getCount(); i++) {
				List<ProcessingOutput> outputs =
					pr instanceof ManualApplicationRecipe mar ? mar.getRollableResults() : pr.getRollableResults();
				for (ItemStack stack : pr.rollResults(outputs)) {
					for (ItemStack previouslyRolled : stacks) {
						if (stack.isEmpty())
							continue;
						if (!ItemHandlerHelper.canItemStacksStack(stack, previouslyRolled))
							continue;
						int amount = Math.min(previouslyRolled.getMaxCount() - previouslyRolled.getCount(),
							stack.getCount());
						previouslyRolled.increment(amount);
						stack.decrement(amount);
					}

					if (stack.isEmpty())
						continue;

					stacks.add(stack);
				}
			}
		} else {
			ItemStack out = recipe.getOutput(level.getRegistryManager())
				.copy();
			stacks = ItemHelper.multipliedOutput(stackIn, out);
		}

		return stacks;
	}
}

package com.simibubi.create.content.kinetics.deployer;

import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

public class ItemApplicationRecipe extends ProcessingRecipe<Inventory> {

	private boolean keepHeldItem;

	public ItemApplicationRecipe(AllRecipeTypes type, ProcessingRecipeParams params) {
		super(type, params);
		keepHeldItem = params.keepHeldItem;
	}

	@Override
	public boolean matches(Inventory inv, World p_77569_2_) {
		return ingredients.get(0)
			.test(inv.getStack(0))
			&& ingredients.get(1)
				.test(inv.getStack(1));
	}

	@Override
	protected int getMaxInputCount() {
		return 2;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	public boolean shouldKeepHeldItem() {
		return keepHeldItem;
	}

	public Ingredient getRequiredHeldItem() {
		if (ingredients.isEmpty())
			throw new IllegalStateException("Item Application Recipe: " + id.toString() + " has no tool!");
		return ingredients.get(1);
	}

	public Ingredient getProcessedItem() {
		if (ingredients.size() < 2)
			throw new IllegalStateException("Item Application Recipe: " + id.toString() + " has no ingredient!");
		return ingredients.get(0);
	}

	@Override
	public void readAdditional(JsonObject json) {
		super.readAdditional(json);
		keepHeldItem = JsonHelper.getBoolean(json, "keepHeldItem", false);
	}

	@Override
	public void writeAdditional(JsonObject json) {
		super.writeAdditional(json);
		if (keepHeldItem)
			json.addProperty("keepHeldItem", keepHeldItem);
	}

	@Override
	public void readAdditional(PacketByteBuf buffer) {
		super.readAdditional(buffer);
		keepHeldItem = buffer.readBoolean();
	}

	@Override
	public void writeAdditional(PacketByteBuf buffer) {
		super.writeAdditional(buffer);
		buffer.writeBoolean(keepHeldItem);
	}

}

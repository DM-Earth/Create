package com.simibubi.create.content.processing.sequenced;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class SequencedAssemblyRecipeSerializer implements RecipeSerializer<SequencedAssemblyRecipe> {

	public SequencedAssemblyRecipeSerializer() {}

	protected void writeToJson(JsonObject json, SequencedAssemblyRecipe recipe) {
		JsonArray nestedRecipes = new JsonArray();
		JsonArray results = new JsonArray();
		json.add("ingredient", recipe.getIngredient().toJson());
		recipe.getSequence().forEach(i -> nestedRecipes.add(i.toJson()));
		recipe.resultPool.forEach(p -> results.add(p.serialize()));
		json.add("transitionalItem", recipe.transitionalItem.serialize());
		json.add("sequence", nestedRecipes);
		json.add("results", results);
		json.addProperty("loops", recipe.loops);
	}

	protected SequencedAssemblyRecipe readFromJson(Identifier recipeId, JsonObject json) {
		SequencedAssemblyRecipe recipe = new SequencedAssemblyRecipe(recipeId, this);
		recipe.ingredient = Ingredient.fromJson(json.get("ingredient"));
		recipe.transitionalItem = ProcessingOutput.deserialize(JsonHelper.getObject(json, "transitionalItem"));
		int i = 0;
		for (JsonElement je : JsonHelper.getArray(json, "sequence"))
			recipe.getSequence().add(SequencedRecipe.fromJson(je.getAsJsonObject(), recipe, i++));
		for (JsonElement je : JsonHelper.getArray(json, "results"))
			recipe.resultPool.add(ProcessingOutput.deserialize(je));
		if (JsonHelper.hasElement(json, "loops"))
			recipe.loops = JsonHelper.getInt(json, "loops");
		return recipe;
	}

	protected void writeToBuffer(PacketByteBuf buffer, SequencedAssemblyRecipe recipe) {
		recipe.getIngredient().write(buffer);
		buffer.writeVarInt(recipe.getSequence().size());
		recipe.getSequence().forEach(sr -> sr.writeToBuffer(buffer));
		buffer.writeVarInt(recipe.resultPool.size());
		recipe.resultPool.forEach(sr -> sr.write(buffer));
		recipe.transitionalItem.write(buffer);
		buffer.writeInt(recipe.loops);
	}

	protected SequencedAssemblyRecipe readFromBuffer(Identifier recipeId, PacketByteBuf buffer) {
		SequencedAssemblyRecipe recipe = new SequencedAssemblyRecipe(recipeId, this);
		recipe.ingredient = Ingredient.fromPacket(buffer);
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			recipe.getSequence().add(SequencedRecipe.readFromBuffer(buffer));
		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			recipe.resultPool.add(ProcessingOutput.read(buffer));
		recipe.transitionalItem = ProcessingOutput.read(buffer);
		recipe.loops = buffer.readInt();
		return recipe;
	}

	public final void write(JsonObject json, SequencedAssemblyRecipe recipe) {
		writeToJson(json, recipe);
	}

	@Override
	public final SequencedAssemblyRecipe read(Identifier id, JsonObject json) {
		return readFromJson(id, json);
	}

	@Override
	public final void write(PacketByteBuf buffer, SequencedAssemblyRecipe recipe) {
		writeToBuffer(buffer, recipe);
	}

	@Override
	public final SequencedAssemblyRecipe read(Identifier id, PacketByteBuf buffer) {
		return readFromBuffer(id, buffer);
	}

}

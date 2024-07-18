package com.simibubi.create.content.processing.sequenced;

import java.util.Arrays;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public class SequencedRecipe<T extends ProcessingRecipe<?>> {

	private T wrapped;

	public SequencedRecipe(T wrapped) {
		this.wrapped = wrapped;
	}

	public IAssemblyRecipe getAsAssemblyRecipe() {
		return (IAssemblyRecipe) wrapped;
	}

	public ProcessingRecipe<?> getRecipe() {
		return wrapped;
	}

	public JsonObject toJson() {
		@SuppressWarnings("unchecked")
		ProcessingRecipeSerializer<T> serializer = (ProcessingRecipeSerializer<T>) wrapped.getSerializer();
		JsonObject json = new JsonObject();
		json.addProperty("type", RegisteredObjects.getKeyOrThrow(serializer)
			.toString());
		serializer.write(json, wrapped);
		return json;
	}

	public static SequencedRecipe<?> fromJson(JsonObject json, SequencedAssemblyRecipe parent, int index) {
		Identifier parentId = parent.getId();
		Recipe<?> recipe = RecipeManager.deserialize(
			new Identifier(parentId.getNamespace(), parentId.getPath() + "_step_" + index), json);
		if (recipe instanceof ProcessingRecipe<?> && recipe instanceof IAssemblyRecipe) {
			ProcessingRecipe<?> processingRecipe = (ProcessingRecipe<?>) recipe;
			IAssemblyRecipe assemblyRecipe = (IAssemblyRecipe) recipe;
			if (assemblyRecipe.supportsAssembly()) {
				Ingredient transit = Ingredient.ofStacks(parent.getTransitionalItem());

				processingRecipe.getIngredients()
					.set(0, index == 0 ? Ingredient.ofEntries(ImmutableList.of(transit, parent.getIngredient()).stream().flatMap(i -> Arrays.stream(i.entries))) : transit);
				SequencedRecipe<?> sequencedRecipe = new SequencedRecipe<>(processingRecipe);
				return sequencedRecipe;
			}
		}
		throw new JsonParseException("Not a supported recipe type");
	}

	public void writeToBuffer(PacketByteBuf buffer) {
		@SuppressWarnings("unchecked")
		ProcessingRecipeSerializer<T> serializer = (ProcessingRecipeSerializer<T>) wrapped.getSerializer();
		buffer.writeIdentifier(RegisteredObjects.getKeyOrThrow(serializer));
		buffer.writeIdentifier(wrapped.getId());
		serializer.write(buffer, wrapped);
	}

	public static SequencedRecipe<?> readFromBuffer(PacketByteBuf buffer) {
		Identifier resourcelocation = buffer.readIdentifier();
		Identifier resourcelocation1 = buffer.readIdentifier();
		RecipeSerializer<?> serializer = Registries.RECIPE_SERIALIZER.get(resourcelocation);
		if (!(serializer instanceof ProcessingRecipeSerializer))
			throw new JsonParseException("Not a supported recipe type");
		@SuppressWarnings("rawtypes")
		ProcessingRecipe recipe = (ProcessingRecipe) serializer.read(resourcelocation1, buffer);
		return new SequencedRecipe<>(recipe);
	}

}

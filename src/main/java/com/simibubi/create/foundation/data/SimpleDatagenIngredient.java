package com.simibubi.create.foundation.data;

import java.util.stream.Stream;
import net.minecraft.recipe.Ingredient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.data.recipe.Mods;

public class SimpleDatagenIngredient extends Ingredient {

	private Mods mod;
	private String id;

	public SimpleDatagenIngredient(Mods mod, String id) {
		super(Stream.empty());
		this.mod = mod;
		this.id = id;
	}

	@Override
	public JsonElement toJson() {
		JsonObject jsonobject = new JsonObject();
		jsonobject.addProperty("item", mod.asResource(id)
			.toString());
		return jsonobject;
	}

}

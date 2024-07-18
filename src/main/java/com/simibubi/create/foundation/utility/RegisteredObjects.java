package com.simibubi.create.foundation.utility;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public final class RegisteredObjects {
	// registry argument for easier porting to 1.19
	@NotNull
	public static <V> Identifier getKeyOrThrow(Registry<V> registry, V value) {
		Identifier key = registry.getId(value);
		if (key == null) {
			throw new IllegalArgumentException("Could not get key for value " + value + "!");
		}
		return key;
	}

	@NotNull
	public static Identifier getKeyOrThrow(Block value) {
		return getKeyOrThrow(Registries.BLOCK, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(Item value) {
		return getKeyOrThrow(Registries.ITEM, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(Fluid value) {
		return getKeyOrThrow(Registries.FLUID, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(EntityType<?> value) {
		return getKeyOrThrow(Registries.ENTITY_TYPE, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(BlockEntityType<?> value) {
		return getKeyOrThrow(Registries.BLOCK_ENTITY_TYPE, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(Potion value) {
		return getKeyOrThrow(Registries.POTION, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(ParticleType<?> value) {
		return getKeyOrThrow(Registries.PARTICLE_TYPE, value);
	}

	@NotNull
	public static Identifier getKeyOrThrow(RecipeSerializer<?> value) {
		return getKeyOrThrow(Registries.RECIPE_SERIALIZER, value);
	}
}

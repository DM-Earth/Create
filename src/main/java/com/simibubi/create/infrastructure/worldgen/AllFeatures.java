package com.simibubi.create.infrastructure.worldgen;

import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.registry.Registries;
import net.minecraft.world.gen.feature.Feature;

public class AllFeatures {
	private static final LazyRegistrar<Feature<?>> REGISTER = LazyRegistrar.create(Registries.FEATURE, Create.ID);

	public static final RegistryObject<LayeredOreFeature> LAYERED_ORE = REGISTER.register("layered_ore", () -> new LayeredOreFeature());

	public static void register() {
		REGISTER.register();
	}
}

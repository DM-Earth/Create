package com.simibubi.create.infrastructure.worldgen;

import com.simibubi.create.Create;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;

public class AllPlacementModifiers {
	private static final LazyRegistrar<PlacementModifierType<?>> REGISTER = LazyRegistrar.create(RegistryKeys.PLACEMENT_MODIFIER_TYPE, Create.ID);

	public static final RegistryObject<PlacementModifierType<ConfigPlacementFilter>> CONFIG_FILTER = REGISTER.register("config_filter", () -> () -> ConfigPlacementFilter.CODEC);

	public static void register() {
		REGISTER.register();
	}
}

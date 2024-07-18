package com.simibubi.create.infrastructure.worldgen;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public class AllBiomeModifiers {
	public static void bootstrap() {
		Predicate<BiomeSelectionContext> isOverworld = BiomeSelectors.foundInOverworld();
		Predicate<BiomeSelectionContext> isNether = BiomeSelectors.foundInTheNether();

		addOre(isOverworld, AllPlacedFeatures.ZINC_ORE);
		addOre(isOverworld, AllPlacedFeatures.STRIATED_ORES_OVERWORLD);
		addOre(isNether, AllPlacedFeatures.STRIATED_ORES_NETHER);
	}

	private static void addOre(Predicate<BiomeSelectionContext> test, RegistryKey<PlacedFeature> feature) {
		BiomeModifications.addFeature(test, Feature.UNDERGROUND_ORES, feature);
	}
}

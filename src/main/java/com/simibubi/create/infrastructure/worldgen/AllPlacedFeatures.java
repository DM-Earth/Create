package com.simibubi.create.infrastructure.worldgen;

import static net.minecraft.world.gen.feature.PlacedFeatures.register;

import java.util.List;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import com.simibubi.create.Create;

public class AllPlacedFeatures {
	public static final RegistryKey<PlacedFeature>
				ZINC_ORE = key("zinc_ore"),
				STRIATED_ORES_OVERWORLD = key("striated_ores_overworld"),
				STRIATED_ORES_NETHER = key("striated_ores_nether");
	
	private static RegistryKey<PlacedFeature> key(String name) {
		return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Create.asResource(name));
	}

	public static void bootstrap(Registerable<PlacedFeature> ctx) {
		RegistryEntryLookup<ConfiguredFeature<?, ?>> featureLookup = ctx.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);
		RegistryEntry<ConfiguredFeature<?, ?>> zincOre = featureLookup.getOrThrow(AllConfiguredFeatures.ZINC_ORE);
		RegistryEntry<ConfiguredFeature<?, ?>> striatedOresOverworld = featureLookup.getOrThrow(AllConfiguredFeatures.STRIATED_ORES_OVERWORLD);
		RegistryEntry<ConfiguredFeature<?, ?>> striatedOresNether = featureLookup.getOrThrow(AllConfiguredFeatures.STRIATED_ORES_NETHER);

		register(ctx, ZINC_ORE, zincOre, placement(CountPlacementModifier.of(8), -63, 70));
		register(ctx, STRIATED_ORES_OVERWORLD, striatedOresOverworld, placement(RarityFilterPlacementModifier.of(18), -30, 70));
		register(ctx, STRIATED_ORES_NETHER, striatedOresNether, placement(RarityFilterPlacementModifier.of(18), 40, 90));
	}

	private static List<PlacementModifier> placement(PlacementModifier frequency, int minHeight, int maxHeight) {
		return List.of(
				frequency,
				SquarePlacementModifier.of(),
				HeightRangePlacementModifier.uniform(YOffset.fixed(minHeight), YOffset.fixed(maxHeight)),
				ConfigPlacementFilter.INSTANCE
		);
	}
}

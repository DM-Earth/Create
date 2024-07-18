package com.simibubi.create.infrastructure.worldgen;

import static net.minecraft.world.gen.feature.ConfiguredFeatures.register;

import java.util.List;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;

public class AllConfiguredFeatures {
	public static final RegistryKey<ConfiguredFeature<?, ?>>
				ZINC_ORE = key("zinc_ore"),
				STRIATED_ORES_OVERWORLD = key("striated_ores_overworld"),
				STRIATED_ORES_NETHER = key("striated_ores_nether");

	private static RegistryKey<ConfiguredFeature<?, ?>> key(String name) {
		return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Create.asResource(name));
	}

	public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> ctx) {
		RuleTest stoneOreReplaceables = new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES);
		RuleTest deepslateOreReplaceables = new TagMatchRuleTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

		List<Target> zincTargetStates = List.of(
			OreFeatureConfig.createTarget(stoneOreReplaceables, AllBlocks.ZINC_ORE.get()
				.getDefaultState()),
			OreFeatureConfig.createTarget(deepslateOreReplaceables, AllBlocks.DEEPSLATE_ZINC_ORE.get()
				.getDefaultState())
		);

		register(ctx, ZINC_ORE, Feature.ORE, new OreFeatureConfig(zincTargetStates, 12));

		List<LayerPattern> overworldLayerPatterns = List.of(
			AllLayerPatterns.SCORIA.get(),
			AllLayerPatterns.CINNABAR.get(),
			AllLayerPatterns.MAGNETITE.get(),
			AllLayerPatterns.MALACHITE.get(),
			AllLayerPatterns.LIMESTONE.get(),
			AllLayerPatterns.OCHRESTONE.get()
		);

		register(ctx, STRIATED_ORES_OVERWORLD, AllFeatures.LAYERED_ORE.get(), new LayeredOreConfiguration(overworldLayerPatterns, 32, 0));

		List<LayerPattern> netherLayerPatterns = List.of(
			AllLayerPatterns.SCORIA_NETHER.get(),
			AllLayerPatterns.SCORCHIA_NETHER.get()
		);

		register(ctx, STRIATED_ORES_NETHER, AllFeatures.LAYERED_ORE.get(), new LayeredOreConfiguration(netherLayerPatterns, 32, 0));
	}
}

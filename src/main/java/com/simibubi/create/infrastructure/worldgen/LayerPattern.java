package com.simibubi.create.infrastructure.worldgen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.utility.Couple;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

public class LayerPattern {
	public static final Codec<LayerPattern> CODEC = Codec.list(Layer.CODEC)
			.xmap(LayerPattern::new, pattern -> pattern.layers);

	public final List<Layer> layers;

	public LayerPattern(List<Layer> layers) {
		this.layers = layers;
	}

	public Layer rollNext(@Nullable Layer previous, Random random) {
		int totalWeight = 0;
		for (Layer layer : layers)
			if (layer != previous)
				totalWeight += layer.weight;
		int rolled = random.nextInt(totalWeight);

		for (Layer layer : layers) {
			if (layer == previous)
				continue;
			rolled -= layer.weight;
			if (rolled < 0)
				return layer;
		}
		return null;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Layer> layers = new ArrayList<>();
		private boolean netherMode;

		public Builder inNether() {
			netherMode = true;
			return this;
		}

		public Builder layer(NonNullConsumer<Layer.Builder> builder) {
			Layer.Builder layerBuilder = new Layer.Builder();
			layerBuilder.netherMode = netherMode;
			builder.accept(layerBuilder);
			layers.add(layerBuilder.build());
			return this;
		}

		public LayerPattern build() {
			return new LayerPattern(layers);
		}
	}

	public static class Layer {
		public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> {
			return instance.group(
				Codec.list(Codec.list(Target.CODEC))
					.fieldOf("targets")
					.forGetter(layer -> layer.targets),
				Codec.intRange(0, Integer.MAX_VALUE)
					.fieldOf("min_size")
					.forGetter(layer -> layer.minSize),
				Codec.intRange(0, Integer.MAX_VALUE)
					.fieldOf("max_size")
					.forGetter(layer -> layer.maxSize),
				Codec.intRange(0, Integer.MAX_VALUE)
					.fieldOf("weight")
					.forGetter(layer -> layer.weight)
			).apply(instance, Layer::new);
		});

		public final List<List<Target>> targets;
		public final int minSize;
		public final int maxSize;
		public final int weight;

		public Layer(List<List<Target>> targets, int minSize, int maxSize, int weight) {
			this.targets = targets;
			this.minSize = minSize;
			this.maxSize = maxSize;
			this.weight = weight;
		}

		public List<Target> rollBlock(Random random) {
			if (targets.size() == 1)
				return targets.get(0);
			return targets.get(random.nextInt(targets.size()));
		}

		public static class Builder {
			private static final RuleTest STONE_ORE_REPLACEABLES = new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES);
			private static final RuleTest DEEPSLATE_ORE_REPLACEABLES = new TagMatchRuleTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
			private static final RuleTest NETHER_ORE_REPLACEABLES = new TagMatchRuleTest(BlockTags.BASE_STONE_NETHER);

			private final List<List<Target>> targets = new ArrayList<>();
			private int minSize = 1;
			private int maxSize = 1;
			private int weight = 1;
			private boolean netherMode;

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder block(NonNullSupplier<? extends Block> block) {
				return block(block.get());
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder passiveBlock() {
				return blocks(Blocks.STONE.getDefaultState(), Blocks.DEEPSLATE.getDefaultState());
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder block(Block block) {
				if (netherMode) {
					this.targets.add(ImmutableList.of(OreFeatureConfig
						.createTarget(NETHER_ORE_REPLACEABLES, block.getDefaultState())));
					return this;
				}
				return blocks(block.getDefaultState(), block.getDefaultState());
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder blocks(Block block, Block deepblock) {
				return blocks(block.getDefaultState(), deepblock.getDefaultState());
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder blocks(Couple<NonNullSupplier<? extends Block>> blocksByDepth) {
				return blocks(blocksByDepth.getFirst()
					.get()
					.getDefaultState(),
					blocksByDepth.getSecond()
						.get()
						.getDefaultState());
			}

			private com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder blocks(BlockState stone, BlockState deepslate) {
				this.targets.add(
					ImmutableList.of(OreFeatureConfig.createTarget(STONE_ORE_REPLACEABLES, stone),
						OreFeatureConfig.createTarget(DEEPSLATE_ORE_REPLACEABLES, deepslate)));
				return this;
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder weight(int weight) {
				this.weight = weight;
				return this;
			}

			public com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer.Builder size(int min, int max) {
				this.minSize = min;
				this.maxSize = max;
				return this;
			}

			public Layer build() {
				return new Layer(targets, minSize, maxSize, weight);
			}
		}
	}
}

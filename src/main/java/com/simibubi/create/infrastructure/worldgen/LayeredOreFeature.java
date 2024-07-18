package com.simibubi.create.infrastructure.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkSectionCache;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import net.minecraft.world.gen.feature.util.FeatureContext;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.worldgen.LayerPattern.Layer;

public class LayeredOreFeature extends Feature<LayeredOreConfiguration> {
	public LayeredOreFeature() {
		super(LayeredOreConfiguration.CODEC);
	}

	@Override
	public boolean generate(FeatureContext<LayeredOreConfiguration> pContext) {
		Random random = pContext.getRandom();
		BlockPos blockpos = pContext.getOrigin();
		StructureWorldAccess worldgenlevel = pContext.getWorld();
		LayeredOreConfiguration config = pContext.getConfig();
		List<LayerPattern> patternPool = config.layerPatterns;

		if (patternPool.isEmpty())
			return false;

		LayerPattern layerPattern = patternPool.get(random.nextInt(patternPool.size()));

		int placedAmount = 0;
		int size = config.size;
		int radius = MathHelper.ceil(config.size / 2f);
		int x0 = blockpos.getX() - radius;
		int y0 = blockpos.getY() - radius;
		int z0 = blockpos.getZ() - radius;
		int width = size + 1;
		int length = size + 1;
		int height = size + 1;

		if (blockpos.getY() >= worldgenlevel.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, blockpos.getX(),
			blockpos.getZ()))
			return false;

		List<LayerPattern.Layer> resolvedLayers = new ArrayList<>();
		List<Float> layerDiameterOffsets = new ArrayList<>();

		Mutable mutablePos = new Mutable();
		ChunkSectionCache bulksectionaccess = new ChunkSectionCache(worldgenlevel);
		int layerCoordinate = random.nextInt(4);
		int slantyCoordinate = random.nextInt(3);
		float slope = random.nextFloat() * .75f;

		try {

			for (int x = 0; x < width; x++) {
				float dx = x * 2f / width - 1;
				if (dx * dx > 1)
					continue;

				for (int y = 0; y < height; y++) {
					float dy = y * 2f / height - 1;
					if (dx * dx + dy * dy > 1)
						continue;
					if (worldgenlevel.isOutOfHeightLimit(y0 + y))
						continue;

					for (int z = 0; z < length; z++) {
						float dz = z * 2f / height - 1;

						int layerIndex = layerCoordinate == 0 ? z : layerCoordinate == 1 ? x : y;
						if (slantyCoordinate != layerCoordinate)
							layerIndex += MathHelper.floor(slantyCoordinate == 0 ? z : slantyCoordinate == 1 ? x : y) * slope;

						while (layerIndex >= resolvedLayers.size()) {
							Layer next = layerPattern.rollNext(
								resolvedLayers.isEmpty() ? null : resolvedLayers.get(resolvedLayers.size() - 1),
								random);
							float offset = random.nextFloat() * .5f + .5f;
							for (int i = 0; i < next.minSize + random.nextInt(1 + next.maxSize - next.minSize); i++) {
								resolvedLayers.add(next);
								layerDiameterOffsets.add(offset);
							}
						}

						if (dx * dx + dy * dy + dz * dz > 1 * layerDiameterOffsets.get(layerIndex))
							continue;

						LayerPattern.Layer layer = resolvedLayers.get(layerIndex);
						List<Target> state = layer.rollBlock(random);

						int currentX = x0 + x;
						int currentY = y0 + y;
						int currentZ = z0 + z;

						mutablePos.set(currentX, currentY, currentZ);
						if (!worldgenlevel.isValidForSetBlock(mutablePos))
							continue;
						ChunkSection levelchunksection = bulksectionaccess.getSection(mutablePos);
						if (levelchunksection == null)
							continue;

						int i3 = ChunkSectionPos.getLocalCoord(currentX);
						int j3 = ChunkSectionPos.getLocalCoord(currentY);
						int k3 = ChunkSectionPos.getLocalCoord(currentZ);
						BlockState blockstate = levelchunksection.getBlockState(i3, j3, k3);

						for (OreFeatureConfig.Target oreconfiguration$targetblockstate : state) {
							if (!canPlaceOre(blockstate, bulksectionaccess::getBlockState, random, config,
								oreconfiguration$targetblockstate, mutablePos))
								continue;
							if (oreconfiguration$targetblockstate.state.isAir())
								continue;
							levelchunksection.setBlockState(i3, j3, k3, oreconfiguration$targetblockstate.state, false);
							++placedAmount;
							break;
						}

					}
				}
			}

		} catch (Throwable throwable1) {
			try {
				bulksectionaccess.close();
			} catch (Throwable throwable) {
				throwable1.addSuppressed(throwable);
			}

			throw throwable1;
		}

		bulksectionaccess.close();
		return placedAmount > 0;
	}

	public boolean canPlaceOre(BlockState pState, Function<BlockPos, BlockState> pAdjacentStateAccessor,
		Random pRandom, LayeredOreConfiguration pConfig, OreFeatureConfig.Target pTargetState,
		BlockPos.Mutable pMatablePos) {
		if (!pTargetState.target.test(pState, pRandom))
			return false;
		if (shouldSkipAirCheck(pRandom, pConfig.discardChanceOnAirExposure))
			return true;

		return !isExposedToAir(pAdjacentStateAccessor, pMatablePos);
	}

	protected boolean shouldSkipAirCheck(Random pRandom, float pChance) {
		return pChance <= 0 ? true : pChance >= 1 ? false : pRandom.nextFloat() >= pChance;
	}
}

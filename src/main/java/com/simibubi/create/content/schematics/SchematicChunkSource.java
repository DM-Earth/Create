package com.simibubi.create.content.schematics;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.tick.EmptyTickSchedulers;
import net.minecraft.world.tick.QueryableTickScheduler;

public class SchematicChunkSource extends ChunkManager {
	private final World fallbackWorld;

	public SchematicChunkSource(World world) {
		fallbackWorld = world;
	}

	@Nullable
	@Override
	public LightSourceView getChunk(int x, int z) {
		return create$getChunk(x, z);
	}

	@Override
	public World getWorld() {
		return fallbackWorld;
	}

	@Nullable
	@Override
	public Chunk getChunk(int x, int z, ChunkStatus status, boolean p_212849_4_) {
		return create$getChunk(x, z);
	}

	public Chunk create$getChunk(int x, int z) {
		return new EmptierChunk(fallbackWorld.getRegistryManager());
	}

	@Override
	public String getDebugString() {
		return "WrappedChunkProvider";
	}

	@Override
	public LightingProvider getLightingProvider() {
		return fallbackWorld.getLightingProvider();
	}

	@Override
	public void tick(BooleanSupplier p_202162_, boolean p_202163_) {}

	@Override
	public int getLoadedChunkCount() {
		return 0;
	}

	public static class EmptierChunk extends WorldChunk {

		private static final class DummyLevel extends World {

			private DummyLevel(MutableWorldProperties pLevelData, RegistryKey<World> pDimension,
				DynamicRegistryManager pRegistryAccess, RegistryEntry<DimensionType> pDimensionTypeRegistration,
				Supplier<Profiler> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
				int pMaxChainedNeighborUpdates) {
				super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug,
					pBiomeZoomSeed, pMaxChainedNeighborUpdates);
				access = pRegistryAccess;
			}

			private final DynamicRegistryManager access;

			private DummyLevel(DynamicRegistryManager access) {
				this(null, null, access, access.get(RegistryKeys.DIMENSION_TYPE)
					.entryOf(DimensionTypes.OVERWORLD), null, false, false, 0, 0);
			}

			@Override
			public ChunkManager getChunkManager() {
				return null;
			}

			@Override
			public void syncWorldEvent(PlayerEntity pPlayer, int pType, BlockPos pPos, int pData) {}

			@Override
			public void emitGameEvent(Entity pEntity, GameEvent pEvent, BlockPos pPos) {}

			@Override
			public void emitGameEvent(GameEvent p_220404_, Vec3d p_220405_, Emitter p_220406_) {}

			@Override
			public DynamicRegistryManager getRegistryManager() {
				return access;
			}

			@Override
			public List<? extends PlayerEntity> getPlayers() {
				return null;
			}

			@Override
			public RegistryEntry<Biome> getGeneratorStoredBiome(int pX, int pY, int pZ) {
				return null;
			}

			@Override
			public float getBrightness(Direction pDirection, boolean pShade) {
				return 0;
			}

			@Override
			public void updateListeners(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {}

			@Override
			public void playSound(PlayerEntity pPlayer, double pX, double pY, double pZ, SoundEvent pSound,
								  SoundCategory pCategory, float pVolume, float pPitch) {}

			@Override
			public void playSoundFromEntity(PlayerEntity pPlayer, Entity pEntity, SoundEvent pEvent, SoundCategory pCategory,
								  float pVolume, float pPitch) {}

			@Override
			public void playSound(PlayerEntity p_220363_, double p_220364_, double p_220365_, double p_220366_,
					SoundEvent p_220367_, SoundCategory p_220368_, float p_220369_, float p_220370_, long p_220371_) {}

			@Override
			public void playSoundFromEntity(PlayerEntity p_220372_, Entity p_220373_, RegistryEntry<SoundEvent> p_220374_, SoundCategory p_220375_,
					float p_220376_, float p_220377_, long p_220378_) {}

			@Override
			public String asString() {
				return null;
			}

			@Override
			public Entity getEntityById(int pId) {
				return null;
			}

			@Override
			public MapState getMapState(String pMapName) {
				return null;
			}

			@Override
			public void putMapState(String pMapId, MapState pData) {}

			@Override
			public int getNextMapId() {
				return 0;
			}

			@Override
			public void setBlockBreakingInfo(int pBreakerId, BlockPos pPos, int pProgress) {}

			@Override
			public Scoreboard getScoreboard() {
				return null;
			}

			@Override
			public RecipeManager getRecipeManager() {
				return null;
			}

			@Override
			protected EntityLookup<Entity> getEntityLookup() {
				return null;
			}

			@Override
			public QueryableTickScheduler<Block> getBlockTickScheduler() {
				return EmptyTickSchedulers.getClientTickScheduler();
			}

			@Override
			public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
				return EmptyTickSchedulers.getClientTickScheduler();
			}

			@Override
			public FeatureSet getEnabledFeatures() {
				return FeatureSet.empty();
			}

			@Override
			public void playSound(PlayerEntity pPlayer, double pX, double pY, double pZ, RegistryEntry<SoundEvent> pSound,
				SoundCategory pSource, float pVolume, float pPitch, long pSeed) {}
		}

		public EmptierChunk(DynamicRegistryManager registryAccess) {
			super(new DummyLevel(registryAccess), null);
		}

		public BlockState getBlockState(BlockPos p_180495_1_) {
			return Blocks.VOID_AIR.getDefaultState();
		}

		@Nullable
		public BlockState setBlockState(BlockPos p_177436_1_, BlockState p_177436_2_, boolean p_177436_3_) {
			return null;
		}

		public FluidState getFluidState(BlockPos p_204610_1_) {
			return Fluids.EMPTY.getDefaultState();
		}

		public int getLuminance(BlockPos p_217298_1_) {
			return 0;
		}

		@Nullable
		public BlockEntity getBlockEntity(BlockPos p_177424_1_, CreationType p_177424_2_) {
			return null;
		}

		public void addBlockEntity(BlockEntity p_150813_1_) {}

		public void setBlockEntity(BlockEntity p_177426_2_) {}

		public void removeBlockEntity(BlockPos p_177425_1_) {}

		public void markUnsaved() {}

		public boolean isEmpty() {
			return true;
		}

		public boolean areSectionsEmptyBetween(int p_76606_1_, int p_76606_2_) {
			return true;
		}

		public ChunkLevelType getLevelType() {
			return ChunkLevelType.FULL;
		}
	}
}

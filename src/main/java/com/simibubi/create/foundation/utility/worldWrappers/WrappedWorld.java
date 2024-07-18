package com.simibubi.create.foundation.utility.worldWrappers;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.tick.QueryableTickScheduler;
import com.simibubi.create.foundation.mixin.accessor.EntityAccessor;

public class WrappedWorld extends World {

	protected World world;
	protected ChunkManager chunkSource;

	protected EntityLookup<Entity> entityGetter = new DummyLevelEntityGetter<>();

	public WrappedWorld(World world) {
		super((MutableWorldProperties) world.getLevelProperties(), world.getRegistryKey(), world.getRegistryManager(), world.getDimensionEntry(),
			world::getProfiler, world.isClient, world.isDebugWorld(), 0, 0);
		this.world = world;
	}

	public void setChunkSource(ChunkManager source) {
		this.chunkSource = source;
	}

	public World getLevel() {
		return world;
	}

	@Override
	public LightingProvider getLightingProvider() {
		return world.getLightingProvider();
	}

	@Override
	public BlockState getBlockState(@Nullable BlockPos pos) {
		return world.getBlockState(pos);
	}

	@Override
	public boolean testBlockState(BlockPos p_217375_1_, Predicate<BlockState> p_217375_2_) {
		return world.testBlockState(p_217375_1_, p_217375_2_);
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return world.getBlockEntity(pos);
	}

	@Override
	public boolean setBlockState(BlockPos pos, BlockState newState, int flags) {
		return world.setBlockState(pos, newState, flags);
	}

	@Override
	public int getLightLevel(BlockPos pos) {
		return 15;
	}

	@Override
	public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
		world.updateListeners(pos, oldState, newState, flags);
	}

	@Override
	public QueryableTickScheduler<Block> getBlockTickScheduler() {
		return world.getBlockTickScheduler();
	}

	@Override
	public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
		return world.getFluidTickScheduler();
	}

	@Override
	public ChunkManager getChunkManager() {
		return chunkSource != null ? chunkSource : world.getChunkManager();
	}

	@Override
	public void syncWorldEvent(@Nullable PlayerEntity player, int type, BlockPos pos, int data) {}

	@Override
	public List<? extends PlayerEntity> getPlayers() {
		return Collections.emptyList();
	}

	@Override
	public void playSound(PlayerEntity p_220363_, double p_220364_, double p_220365_, double p_220366_,
		SoundEvent p_220367_, SoundCategory p_220368_, float p_220369_, float p_220370_, long p_220371_) {}

	@Override
	public void playSound(PlayerEntity pPlayer, double pX, double pY, double pZ, RegistryEntry<SoundEvent> pSound,
		SoundCategory pSource, float pVolume, float pPitch, long pSeed) {}

	@Override
	public void playSoundFromEntity(PlayerEntity pPlayer, Entity pEntity, RegistryEntry<SoundEvent> pSound, SoundCategory pCategory,
		float pVolume, float pPitch, long pSeed) {}

	@Override
	public void playSound(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent soundIn,
		SoundCategory category, float volume, float pitch) {}

	@Override
	public void playSoundFromEntity(@Nullable PlayerEntity p_217384_1_, Entity p_217384_2_, SoundEvent p_217384_3_,
		SoundCategory p_217384_4_, float p_217384_5_, float p_217384_6_) {}

	@Override
	public Entity getEntityById(int id) {
		return null;
	}

	@Override
	public MapState getMapState(String mapName) {
		return null;
	}

	@Override
	public boolean spawnEntity(Entity entityIn) {
		((EntityAccessor) entityIn).create$callSetWorld(world);
		return world.spawnEntity(entityIn);
	}

	@Override
	public void putMapState(String pMapId, MapState pData) {}

	@Override
	public int getNextMapId() {
		return world.getNextMapId();
	}

	@Override
	public void setBlockBreakingInfo(int breakerId, BlockPos pos, int progress) {}

	@Override
	public Scoreboard getScoreboard() {
		return world.getScoreboard();
	}

	@Override
	public RecipeManager getRecipeManager() {
		return world.getRecipeManager();
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_) {
		return world.getGeneratorStoredBiome(p_225604_1_, p_225604_2_, p_225604_3_);
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return world.getRegistryManager();
	}

	@Override
	public float getBrightness(Direction p_230487_1_, boolean p_230487_2_) {
		return world.getBrightness(p_230487_1_, p_230487_2_);
	}

	@Override
	public void updateComparators(BlockPos p_175666_1_, Block p_175666_2_) {}

	@Override
	public void emitGameEvent(Entity pEntity, GameEvent pEvent, BlockPos pPos) {}

	@Override
	public void emitGameEvent(GameEvent p_220404_, Vec3d p_220405_, Emitter p_220406_) {}

	@Override
	public String asString() {
		return world.asString();
	}

	@Override
	protected EntityLookup<Entity> getEntityLookup() {
		return entityGetter;
	}

	// Intentionally copied from LevelHeightAccessor. Workaround for issues caused
	// when other mods (such as Lithium)
	// override the vanilla implementations in ways which cause WrappedWorlds to
	// return incorrect, default height info.
	// WrappedWorld subclasses should implement their own getMinBuildHeight and
	// getHeight overrides where they deviate
	// from the defaults for their dimension.

	@Override
	public int getTopY() {
		return this.getBottomY() + this.getHeight();
	}

	@Override
	public int countVerticalSections() {
		return this.getTopSectionCoord() - this.getBottomSectionCoord();
	}

	@Override
	public int getBottomSectionCoord() {
		return ChunkSectionPos.getSectionCoord(this.getBottomY());
	}

	@Override
	public int getTopSectionCoord() {
		return ChunkSectionPos.getSectionCoord(this.getTopY() - 1) + 1;
	}

	@Override
	public boolean isOutOfHeightLimit(BlockPos pos) {
		return this.isOutOfHeightLimit(pos.getY());
	}

	@Override
	public boolean isOutOfHeightLimit(int y) {
		return y < this.getBottomY() || y >= this.getTopY();
	}

	@Override
	public int getSectionIndex(int y) {
		return this.sectionCoordToIndex(ChunkSectionPos.getSectionCoord(y));
	}

	@Override
	public int sectionCoordToIndex(int sectionY) {
		return sectionY - this.getBottomSectionCoord();
	}

	@Override
	public int sectionIndexToCoord(int sectionIndex) {
		return sectionIndex + this.getBottomSectionCoord();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return world.getEnabledFeatures();
	}

}

package com.simibubi.create.foundation.utility.worldWrappers;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.tick.WorldTickScheduler;
import com.simibubi.create.foundation.mixin.accessor.EntityAccessor;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.MinecraftServerAccessor;
import io.github.fabricators_of_create.porting_lib.util.BiomeManagerHelper;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WrappedServerWorld extends ServerWorld {

	protected ServerWorld world;

	public WrappedServerWorld(ServerWorld world) {
		super(world.getServer(), Util.getMainWorkerExecutor(),  ((MinecraftServerAccessor) world.getServer()).port_lib$getStorageSource(),
			(ServerWorldProperties) world.getLevelProperties(), world.getRegistryKey(),
			new DimensionOptions(world.getDimensionEntry(), world.getChunkManager().getChunkGenerator()),
			new DummyStatusListener(), world.isDebugWorld(), BiomeManagerHelper.getSeed(world.getBiomeAccess()),
			Collections.emptyList(), false, world.getRandomSequences());
		this.world = world;
	}

	@Override
	public float getSkyAngleRadians(float p_72826_1_) {
		return 0;
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
	public WorldTickScheduler<Block> getBlockTickScheduler() {
		return super.getBlockTickScheduler();
	}

	@Override
	public WorldTickScheduler<Fluid> getFluidTickScheduler() {
		return super.getFluidTickScheduler();
	}

	@Override
	public void syncWorldEvent(PlayerEntity player, int type, BlockPos pos, int data) {}

	@Override
	public List<ServerPlayerEntity> getPlayers() {
		return Collections.emptyList();
	}

	@Override
	public void playSound(PlayerEntity player, double x, double y, double z, SoundEvent soundIn, SoundCategory category,
		float volume, float pitch) {}

	@Override
	public void playSoundFromEntity(PlayerEntity p_217384_1_, Entity p_217384_2_, SoundEvent p_217384_3_, SoundCategory p_217384_4_,
		float p_217384_5_, float p_217384_6_) {}

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
	public void putMapState(String mapId, MapState mapDataIn) {}

	@Override
	public int getNextMapId() {
		return 0;
	}

	@Override
	public void setBlockBreakingInfo(int breakerId, BlockPos pos, int progress) {}

	@Override
	public RecipeManager getRecipeManager() {
		return world.getRecipeManager();
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_) {
		return world.getGeneratorStoredBiome(p_225604_1_, p_225604_2_, p_225604_3_);
	}

}

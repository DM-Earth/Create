package com.simibubi.create.foundation.utility.worldWrappers;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.ClientPacketListenerAccessor;
import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.BiomeManagerAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;

@Environment(EnvType.CLIENT)
@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
public class WrappedClientWorld extends ClientWorld {
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	protected World world;

	private WrappedClientWorld(World world) {
		super(mc.getNetworkHandler(), mc.world.getLevelProperties(), world.getRegistryKey(), world.getDimensionEntry(),
			((ClientPacketListenerAccessor) mc.getNetworkHandler()).port_lib$getServerChunkRadius(), mc.world.getSimulationDistance(), world.getProfilerSupplier(),
			mc.worldRenderer, world.isDebugWorld(), ((BiomeManagerAccessor) world.getBiomeAccess()).port_lib$getBiomeZoomSeed());
		this.world = world;
	}

	public static WrappedClientWorld of(World world) {
		return new WrappedClientWorld(world);
	}

	@Override
	public boolean isChunkLoaded(BlockPos pos) {
		return world.isChunkLoaded(pos);
	}

	@Override
	public boolean canSetBlock(BlockPos pos) {
		return world.canSetBlock(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	// FIXME: blockstate#getCollisionShape with WrappedClientWorld gives unreliable
	// data (maybe)

	@Override
	public int getLightLevel(LightType type, BlockPos pos) {
		return world.getLightLevel(type, pos);
	}

	@Override
	public int getLuminance(BlockPos pos) {
		return world.getLuminance(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return world.getFluidState(pos);
	}

	@Nullable
	@Override
	public <T extends LivingEntity> T getClosestEntity(List<? extends T> p_217361_1_, TargetPredicate p_217361_2_,
		@Nullable LivingEntity p_217361_3_, double p_217361_4_, double p_217361_6_, double p_217361_8_) {
		return world.getClosestEntity(p_217361_1_, p_217361_2_, p_217361_3_, p_217361_4_, p_217361_6_, p_217361_8_);
	}

	@Override
	public int getColor(BlockPos p_225525_1_, ColorResolver p_225525_2_) {
		return world.getColor(p_225525_1_, p_225525_2_);
	}

	// FIXME: Emissive Lighting might not light stuff properly

	@Override
	public void addParticle(ParticleEffect p_195594_1_, double p_195594_2_, double p_195594_4_, double p_195594_6_,
		double p_195594_8_, double p_195594_10_, double p_195594_12_) {
		world.addParticle(p_195594_1_, p_195594_2_, p_195594_4_, p_195594_6_, p_195594_8_, p_195594_10_, p_195594_12_);
	}

	@Override
	public void addParticle(ParticleEffect p_195590_1_, boolean p_195590_2_, double p_195590_3_, double p_195590_5_,
		double p_195590_7_, double p_195590_9_, double p_195590_11_, double p_195590_13_) {
		world.addParticle(p_195590_1_, p_195590_2_, p_195590_3_, p_195590_5_, p_195590_7_, p_195590_9_, p_195590_11_,
			p_195590_13_);
	}

	@Override
	public void addImportantParticle(ParticleEffect p_195589_1_, double p_195589_2_, double p_195589_4_,
		double p_195589_6_, double p_195589_8_, double p_195589_10_, double p_195589_12_) {
		world.addImportantParticle(p_195589_1_, p_195589_2_, p_195589_4_, p_195589_6_, p_195589_8_, p_195589_10_,
			p_195589_12_);
	}

	@Override
	public void addImportantParticle(ParticleEffect p_217404_1_, boolean p_217404_2_, double p_217404_3_,
		double p_217404_5_, double p_217404_7_, double p_217404_9_, double p_217404_11_, double p_217404_13_) {
		world.addImportantParticle(p_217404_1_, p_217404_2_, p_217404_3_, p_217404_5_, p_217404_7_, p_217404_9_,
			p_217404_11_, p_217404_13_);
	}

	@Override
	public void playSound(double p_184134_1_, double p_184134_3_, double p_184134_5_, SoundEvent p_184134_7_,
		SoundCategory p_184134_8_, float p_184134_9_, float p_184134_10_, boolean p_184134_11_) {
		world.playSound(p_184134_1_, p_184134_3_, p_184134_5_, p_184134_7_, p_184134_8_, p_184134_9_, p_184134_10_,
			p_184134_11_);
	}

	@Override
	public void playSound(@Nullable PlayerEntity p_184148_1_, double p_184148_2_, double p_184148_4_, double p_184148_6_,
		SoundEvent p_184148_8_, SoundCategory p_184148_9_, float p_184148_10_, float p_184148_11_) {
		world.playSound(p_184148_1_, p_184148_2_, p_184148_4_, p_184148_6_, p_184148_8_, p_184148_9_, p_184148_10_,
			p_184148_11_);
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos p_175625_1_) {
		return world.getBlockEntity(p_175625_1_);
	}

	public World getWrappedWorld() {
		return world;
	}
}

package com.simibubi.create.content.equipment.bell;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BasicParticleData<T extends Particle> implements ParticleEffect, ICustomParticleDataWithSprite<BasicParticleData<T>> {

	public BasicParticleData() { }

	@Override
	public Factory<BasicParticleData<T>> getDeserializer() {
		BasicParticleData<T> data = this;
		return new ParticleEffect.Factory<BasicParticleData<T>>() {
			@Override
			public BasicParticleData<T> read(ParticleType<BasicParticleData<T>> arg0, StringReader reader) {
				return data;
			}

			@Override
			public BasicParticleData<T> read(ParticleType<BasicParticleData<T>> type, PacketByteBuf buffer) {
				return data;
			}
		};
	}

	@Override
	public Codec<BasicParticleData<T>> getCodec(ParticleType<BasicParticleData<T>> type) {
		return Codec.unit(this);
	}

	public interface IBasicParticleFactory<U extends Particle> {
		U makeParticle(ClientWorld worldIn, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprite);
	}

	@Environment(EnvType.CLIENT)
	public abstract IBasicParticleFactory<T> getBasicFactory();

	@Override
	@Environment(EnvType.CLIENT)
	public ParticleManager.SpriteAwareFactory<BasicParticleData<T>> getMetaFactory() {
		return animatedSprite -> (data, worldIn, x, y, z, vx, vy, vz) ->
				getBasicFactory().makeParticle(worldIn, x, y, z, vx, vy, vz, animatedSprite);
	}

	@Override
	public String asString() {
		return RegisteredObjects.getKeyOrThrow(getType()).toString();
	}

	@Override
	public void write(PacketByteBuf buffer) { }
}

package com.simibubi.create.foundation.particle;

import com.mojang.serialization.Codec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleEffect.Factory;
import net.minecraft.particle.ParticleType;

public interface ICustomParticleData<T extends ParticleEffect> {

	Factory<T> getDeserializer();

	Codec<T> getCodec(ParticleType<T> type);

	public default ParticleType<T> createType() {
		return new ParticleType<T>(false, getDeserializer()) {

			@Override
			public Codec<T> getCodec() {
				return ICustomParticleData.this.getCodec(this);
			}
		};
	}

	@Environment(EnvType.CLIENT)
	public ParticleFactory<T> getFactory();

	@Environment(EnvType.CLIENT)
	public default void register(ParticleType<T> type, ParticleManager particles) {
		ParticleFactoryRegistry.getInstance().register(type, getFactory());
	}

}

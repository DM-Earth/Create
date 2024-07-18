package com.simibubi.create.foundation.particle;

import com.mojang.serialization.Codec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleManager.SpriteAwareFactory;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleEffect.Factory;
import net.minecraft.particle.ParticleType;

public interface ICustomParticleDataWithSprite<T extends ParticleEffect> extends ICustomParticleData<T> {

	Factory<T> getDeserializer();

	public default ParticleType<T> createType() {
		return new ParticleType<T>(false, getDeserializer()) {

			@Override
			public Codec<T> getCodec() {
				return ICustomParticleDataWithSprite.this.getCodec(this);
			}
		};
	}

	@Override
	@Environment(EnvType.CLIENT)
	default ParticleFactory<T> getFactory() {
		throw new IllegalAccessError("This particle type uses a metaFactory!");
	}

	@Environment(EnvType.CLIENT)
	public SpriteAwareFactory<T> getMetaFactory();

	@Override
	@Environment(EnvType.CLIENT)
	public default void register(ParticleType<T> type, ParticleManager particles) {
		ParticleFactoryRegistry.getInstance().register(type, getMetaFactory()::create);
	}
}

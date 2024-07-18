package com.simibubi.create.content.kinetics.steamEngine;

import java.util.Locale;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleManager.SpriteAwareFactory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public class SteamJetParticleData implements ParticleEffect, ICustomParticleDataWithSprite<SteamJetParticleData> {

	public static final Codec<SteamJetParticleData> CODEC = RecordCodecBuilder.create(i -> i
		.group(Codec.FLOAT.fieldOf("speed")
			.forGetter(p -> p.speed))
		.apply(i, SteamJetParticleData::new));

	public static final ParticleEffect.Factory<SteamJetParticleData> DESERIALIZER =
		new ParticleEffect.Factory<SteamJetParticleData>() {
			public SteamJetParticleData read(ParticleType<SteamJetParticleData> particleTypeIn,
				StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				float speed = reader.readFloat();
				return new SteamJetParticleData(speed);
			}

			public SteamJetParticleData read(ParticleType<SteamJetParticleData> particleTypeIn,
				PacketByteBuf buffer) {
				return new SteamJetParticleData(buffer.readFloat());
			}
		};

	float speed;

	public SteamJetParticleData(float speed) {
		this.speed = speed;
	}

	public SteamJetParticleData() {
		this(0);
	}

	@Override
	public ParticleType<?> getType() {
		return AllParticleTypes.STEAM_JET.get();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeFloat(speed);
	}

	@Override
	public String asString() {
		return String.format(Locale.ROOT, "%s %f", AllParticleTypes.STEAM_JET.parameter(), speed);
	}

	@Override
	public Factory<SteamJetParticleData> getDeserializer() {
		return DESERIALIZER;
	}

	@Override
	public Codec<SteamJetParticleData> getCodec(ParticleType<SteamJetParticleData> type) {
		return CODEC;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public SpriteAwareFactory<SteamJetParticleData> getMetaFactory() {
		return SteamJetParticle.Factory::new;
	}

}
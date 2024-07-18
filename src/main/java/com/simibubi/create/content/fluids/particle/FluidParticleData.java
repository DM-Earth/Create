package com.simibubi.create.content.fluids.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.foundation.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.RegisteredObjects;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public class FluidParticleData implements ParticleEffect, ICustomParticleData<FluidParticleData> {

	private ParticleType<FluidParticleData> type;
	private FluidStack fluid;

	public FluidParticleData() {}

	@SuppressWarnings("unchecked")
	public FluidParticleData(ParticleType<?> type, FluidStack fluid) {
		this.type = (ParticleType<FluidParticleData>) type;
		this.fluid = fluid;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public ParticleFactory<FluidParticleData> getFactory() {
		return this::create;
	}

	// fabric: lambda funk
	@Environment(EnvType.CLIENT)
	private Particle create(FluidParticleData type, ClientWorld level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		return FluidStackParticle.create(type.type, level, type.fluid, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	@Override
	public ParticleType<?> getType() {
		return type;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		fluid.writeToPacket(buffer);
	}

	@Override
	public String asString() {
		return RegisteredObjects.getKeyOrThrow(type) + " " + RegisteredObjects.getKeyOrThrow(fluid.getFluid());
	}

	public static final Codec<FluidParticleData> CODEC = RecordCodecBuilder.create(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_PARTICLE.get(), fs)));

	public static final Codec<FluidParticleData> BASIN_CODEC = RecordCodecBuilder.create(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), fs)));

	public static final Codec<FluidParticleData> DRIP_CODEC = RecordCodecBuilder.create(i -> i
		.group(FluidStack.CODEC.fieldOf("fluid")
			.forGetter(p -> p.fluid))
		.apply(i, fs -> new FluidParticleData(AllParticleTypes.FLUID_DRIP.get(), fs)));

	public static final ParticleEffect.Factory<FluidParticleData> DESERIALIZER =
		new ParticleEffect.Factory<FluidParticleData>() {

			// TODO Fluid particles on command
			public FluidParticleData read(ParticleType<FluidParticleData> particleTypeIn, StringReader reader)
				throws CommandSyntaxException {
				return new FluidParticleData(particleTypeIn, new FluidStack(Fluids.WATER, 1));
			}

			public FluidParticleData read(ParticleType<FluidParticleData> particleTypeIn, PacketByteBuf buffer) {
				return new FluidParticleData(particleTypeIn, FluidStack.readFromPacket(buffer));
			}
		};

	@Override
	public Factory<FluidParticleData> getDeserializer() {
		return DESERIALIZER;
	}

	@Override
	public Codec<FluidParticleData> getCodec(ParticleType<FluidParticleData> type) {
		if (type == AllParticleTypes.BASIN_FLUID.get())
			return BASIN_CODEC;
		if (type == AllParticleTypes.FLUID_DRIP.get())
			return DRIP_CODEC;
		return CODEC;
	}

}

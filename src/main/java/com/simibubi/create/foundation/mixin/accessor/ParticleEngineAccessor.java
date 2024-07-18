package com.simibubi.create.foundation.mixin.accessor;

import java.util.Map;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleManager.class)
public interface ParticleEngineAccessor {
	// This field cannot be ATed because its type is patched by Forge
	@Accessor("factories")
	Int2ObjectMap<ParticleFactory<?>> create$getFactories();
}

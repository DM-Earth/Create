package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
	@Invoker("setWorld")
	void create$callSetWorld(World world);
}

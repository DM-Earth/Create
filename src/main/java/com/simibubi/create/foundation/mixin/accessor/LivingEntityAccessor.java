package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Invoker("spawnItemParticles")
	void create$callSpawnItemParticles(ItemStack stack, int count);
}

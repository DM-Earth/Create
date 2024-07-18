package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ProjectileDispenserBehavior.class)
public interface AbstractProjectileDispenseBehaviorAccessor {
	@Invoker("createProjectile")
	ProjectileEntity create$callCreateProjectile(World level, Position position, ItemStack stack);

	@Invoker("getVariation")
	float create$callGetVariation();

	@Invoker("getForce")
	float create$callGetForce();
}

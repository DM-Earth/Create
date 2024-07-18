package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
	@Invoker("<init>")
	static FallingBlockEntity create$callInit(World level, double x, double y, double z, BlockState state) {
		throw new AssertionError();
	}
}

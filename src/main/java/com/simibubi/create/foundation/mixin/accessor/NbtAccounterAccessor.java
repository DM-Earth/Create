package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.nbt.NbtTagSizeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NbtTagSizeTracker.class)
public interface NbtAccounterAccessor {
	@Accessor("allocatedBytes")
	long create$getAllocatedBytes();
}

package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.EntityList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerWorld.class)
public interface ServerLevelAccessor {
	@Accessor("entityList")
	EntityList create$getEntityList();
}

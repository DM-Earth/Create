package com.simibubi.create.foundation.mixin.fabric;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayNetworkHandler.class)
public interface ServerGamePacketListenerImplAccessor {
	@Accessor("floatingTicks")
	void create$setFloatingTicks(int ticks);

	@Accessor("vehicleFloatingTicks")
	void create$setVehicleFloatingTicks(int ticks);
}

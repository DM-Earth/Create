package com.simibubi.create.foundation.mixin.fabric;

import com.simibubi.create.foundation.utility.AttachedRegistry;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Registries.class)
public class RegistryMixin {
	@Inject(method = "freezeRegistries", at = @At("TAIL"))
	private static void unwrapAttached(CallbackInfo ci) {
		AttachedRegistry.unwrapAll();
	}
}

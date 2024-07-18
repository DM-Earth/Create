package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class HeavyBootsOnPlayerMixin extends AbstractClientPlayerEntity {

	private HeavyBootsOnPlayerMixin(ClientWorld level, GameProfile profile) {
		super(level, profile);
	}

	@Inject(method = "isSubmergedInWater()Z", at = @At("HEAD"), cancellable = true)
	public void create$noSwimmingWithHeavyBootsOn(CallbackInfoReturnable<Boolean> cir) {
		NbtCompound persistentData = getCustomData();
		if (persistentData.contains("HeavyBoots"))
			cir.setReturnValue(false);
	}
}

package com.simibubi.create.foundation.mixin.fabric;

import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.foundation.utility.fabric.AbstractMinecartExtensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartMixin implements AbstractMinecartExtensions {
	@Unique
	private MinecartController controller;

	@Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
	private void initController(EntityType<?> entityType, World level, CallbackInfo ci) {
		this.controller = new MinecartController((AbstractMinecartEntity) (Object) this);
		if (level != null) { // don't trust modders
			CapabilityMinecartController.attach((AbstractMinecartEntity) (Object) this);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
	private void loadController(NbtCompound compound, CallbackInfo ci) {
		if (compound.contains(CAP_KEY, NbtElement.COMPOUND_TYPE))
			controller.deserializeNBT(compound.getCompound(CAP_KEY));
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	private void saveController(NbtCompound compound, CallbackInfo ci) {
		compound.put(CAP_KEY, controller.serializeNBT());
	}

	@Override
	public MinecartController create$getController() {
		return controller;
	}
}

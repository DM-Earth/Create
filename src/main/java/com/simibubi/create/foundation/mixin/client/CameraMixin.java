package com.simibubi.create.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.simibubi.create.content.trains.CameraDistanceModifier;
import net.minecraft.client.render.Camera;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyArg(
			method = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(D)D"),
			index = 0
	)
	public double create$modifyCameraOffset(double originalValue) {
		return originalValue * CameraDistanceModifier.getMultiplier();
	}
}

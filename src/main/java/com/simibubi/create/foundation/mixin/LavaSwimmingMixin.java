package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class LavaSwimmingMixin extends Entity {
	private LavaSwimmingMixin(EntityType<?> type, World level) {
		super(type, level);
	}

	@Inject(method = "travel(Lnet/minecraft/util/math/Vec3d;)V", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isInLava()Z")), at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", shift = Shift.AFTER, ordinal = 0))
	private void create$onLavaTravel(Vec3d travelVector, CallbackInfo ci) {
		ItemStack bootsStack = DivingBootsItem.getWornItem(this);
		if (AllItems.NETHERITE_DIVING_BOOTS.isIn(bootsStack))
			setVelocity(getVelocity().multiply(DivingBootsItem.getMovementMultiplier((LivingEntity) (Object) this)));
	}
}

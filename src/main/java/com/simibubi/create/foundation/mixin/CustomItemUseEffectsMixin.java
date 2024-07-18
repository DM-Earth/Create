package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.item.CustomUseEffectsItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(LivingEntity.class)
public abstract class CustomItemUseEffectsMixin extends Entity {
	private CustomItemUseEffectsMixin(EntityType<?> entityType, World level) {
		super(entityType, level);
	}

	@Shadow
	public abstract ItemStack getActiveItem();

	@Inject(method = "shouldSpawnConsumptionEffects()Z", at = @At("HEAD"), cancellable = true)
	private void create$onShouldTriggerUseEffects(CallbackInfoReturnable<Boolean> cir) {
		ItemStack using = getActiveItem();
		Item item = using.getItem();
		if (item instanceof CustomUseEffectsItem handler) {
			Boolean result = handler.shouldTriggerUseEffects(using, (LivingEntity) (Object) this);
			if (result != null) {
				cir.setReturnValue(result);
			}
		}
	}

	@Inject(method = "spawnConsumptionEffects(Lnet/minecraft/item/ItemStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;", ordinal = 0), cancellable = true)
	private void create$onTriggerUseEffects(ItemStack stack, int count, CallbackInfo ci) {
		Item item = stack.getItem();
		if (item instanceof CustomUseEffectsItem handler) {
			if (handler.triggerUseEffects(stack, (LivingEntity) (Object) this, count, random)) {
				ci.cancel();
			}
		}
	}
}

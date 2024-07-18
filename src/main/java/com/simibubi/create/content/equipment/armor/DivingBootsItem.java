package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.utility.NBTHelper;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.LivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class DivingBootsItem extends BaseArmorItem {
	public static final EquipmentSlot SLOT = EquipmentSlot.FEET;
	public static final ArmorItem.Type TYPE = ArmorItem.Type.BOOTS;

	public DivingBootsItem(ArmorMaterial material, Settings properties, Identifier textureLoc) {
		super(material, TYPE, properties, textureLoc);
	}

	public static boolean isWornBy(Entity entity) {
		return !getWornItem(entity).isEmpty();
	}

	public static ItemStack getWornItem(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = livingEntity.getEquippedStack(SLOT);
		if (!(stack.getItem() instanceof DivingBootsItem)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}

	public static void accellerateDescentUnderwater(LivingEntity entity) {
//		LivingEntity entity = event.getEntityLiving();
		if (!affects(entity))
			return;

		Vec3d motion = entity.getVelocity();
		boolean isJumping = ((LivingEntityAccessor) entity).port_lib$isJumping();
		entity.setOnGround(entity.isOnGround() || entity.verticalCollision);

		if (isJumping && entity.isOnGround()) {
			motion = motion.add(0, .5f, 0);
			entity.setOnGround(false);
		} else {
			motion = motion.add(0, -0.05f, 0);
		}

		float multiplier = 1.3f;
		if (motion.multiply(1, 0, 1)
			.length() < 0.145f && (entity.forwardSpeed > 0 || entity.sidewaysSpeed != 0) && !entity.isSneaking())
			motion = motion.multiply(multiplier, 1, multiplier);
		entity.setVelocity(motion);
	}

	protected static boolean affects(LivingEntity entity) {
		if (!isWornBy(entity)) {
			entity.getCustomData()
				.remove("HeavyBoots");
			return false;
		}

		NBTHelper.putMarker(entity.getCustomData(), "HeavyBoots");
		if (!entity.isTouchingWater())
			return false;
		if (entity.getPose() == EntityPose.SWIMMING)
			return false;
		if (entity instanceof PlayerEntity) {
			PlayerEntity playerEntity = (PlayerEntity) entity;
			if (playerEntity.getAbilities().flying)
				return false;
		}
		return true;
	}

	public static Vec3d getMovementMultiplier(LivingEntity entity) {
		double yMotion = entity.getVelocity().y;
		double vMultiplier = yMotion < 0 ? Math.max(0, 2.5 - Math.abs(yMotion) * 2) : 1;

		if (!entity.isOnGround()) {
			if (((LivingEntityAccessor) entity).port_lib$isJumping() && entity.getCustomData()
				.contains("LavaGrounded")) {
				boolean eyeInFluid = entity.isSubmergedIn(FluidTags.LAVA);
				vMultiplier = yMotion == 0 ? 0 : (eyeInFluid ? 1 : 0.5) / yMotion;
			} else if (yMotion > 0)
				vMultiplier = 1.3;

			entity.getCustomData()
				.remove("LavaGrounded");
			return new Vec3d(1.75, vMultiplier, 1.75);
		}

		entity.getCustomData()
			.putBoolean("LavaGrounded", true);
		double hMultiplier = entity.isSprinting() ? 1.85 : 1.75;
		return new Vec3d(hMultiplier, vMultiplier, hMultiplier);
	}

}

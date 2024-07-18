package com.simibubi.create.content.equipment.goggles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import com.simibubi.create.AllItems;

public class GogglesItem extends Item {

	private static final List<Predicate<PlayerEntity>> IS_WEARING_PREDICATES = new ArrayList<>();
	static {
		addIsWearingPredicate(player -> AllItems.GOGGLES.isIn(player.getEquippedStack(EquipmentSlot.HEAD)));
	}

	public GogglesItem(Settings properties) {
		super(properties);
		DispenserBlock.registerBehavior(this, ArmorItem.DISPENSER_BEHAVIOR);
	}

	// Set in properties
	public static EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	public TypedActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack itemstack = playerIn.getStackInHand(handIn);
		EquipmentSlot equipmentslottype = MobEntity.getPreferredEquipmentSlot(itemstack);
		ItemStack itemstack1 = playerIn.getEquippedStack(equipmentslottype);
		if (itemstack1.isEmpty()) {
			playerIn.equipStack(equipmentslottype, itemstack.copy());
			itemstack.setCount(0);
			return new TypedActionResult<>(ActionResult.SUCCESS, itemstack);
		} else {
			return new TypedActionResult<>(ActionResult.FAIL, itemstack);
		}
	}

	public static boolean isWearingGoggles(PlayerEntity player) {
		for (Predicate<PlayerEntity> predicate : IS_WEARING_PREDICATES) {
			if (predicate.test(player)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Use this method to add custom entry points to the goggles overlay, e.g. custom
	 * armor, handheld alternatives, etc.
	 */
	public static void addIsWearingPredicate(Predicate<PlayerEntity> predicate) {
		IS_WEARING_PREDICATES.add(predicate);
	}

}

package com.simibubi.create.content.equipment.armor;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingTableBehaviorEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class CapacityEnchantment extends Enchantment implements CustomEnchantingTableBehaviorEnchantment {

	public CapacityEnchantment(Rarity rarity, EnchantmentTarget category, EquipmentSlot[] slots) {
		super(rarity, category, slots);
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return stack.getItem() instanceof ICapacityEnchantable;
	}

	@Override
	public boolean isAcceptableItem(ItemStack stack) {
		return canApplyAtEnchantingTable(stack);
	}

	public interface ICapacityEnchantable {
	}

}

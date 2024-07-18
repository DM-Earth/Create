package com.simibubi.create.content.equipment.potatoCannon;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingTableBehaviorEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class PotatoRecoveryEnchantment extends Enchantment implements CustomEnchantingTableBehaviorEnchantment {

	public PotatoRecoveryEnchantment(Rarity p_i46731_1_, EnchantmentTarget p_i46731_2_, EquipmentSlot[] p_i46731_3_) {
		super(p_i46731_1_, p_i46731_2_, p_i46731_3_);
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return stack.getItem() instanceof PotatoCannonItem;
	}

	@Override
	public boolean isAcceptableItem(ItemStack stack) {
		return canApplyAtEnchantingTable(stack);
	}
}

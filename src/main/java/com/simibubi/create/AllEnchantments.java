package com.simibubi.create;

import static com.simibubi.create.Create.REGISTRATE;

import com.simibubi.create.content.equipment.armor.CapacityEnchantment;
import com.simibubi.create.content.equipment.potatoCannon.PotatoRecoveryEnchantment;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.enchantment.Enchantment.Rarity;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class AllEnchantments {

	public static final RegistryEntry<PotatoRecoveryEnchantment> POTATO_RECOVERY = REGISTRATE.object("potato_recovery")
		.enchantment(EnchantmentTarget.BOW, PotatoRecoveryEnchantment::new)
		.addSlots(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)
		.lang("Potato Recovery")
		.rarity(Rarity.UNCOMMON)
		.register();
	
	public static final RegistryEntry<CapacityEnchantment> CAPACITY = REGISTRATE.object("capacity")
		.enchantment(EnchantmentTarget.ARMOR_CHEST, CapacityEnchantment::new)
		.addSlots(EquipmentSlot.CHEST)
		.lang("Capacity")
		.rarity(Rarity.COMMON)
		.register();

	public static void register() {}

}

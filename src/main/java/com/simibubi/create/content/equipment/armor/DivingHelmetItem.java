package com.simibubi.create.content.equipment.armor;

import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.foundation.advancement.AllAdvancements;

import io.github.fabricators_of_create.porting_lib.enchant.CustomEnchantingBehaviorItem;
import io.github.fabricators_of_create.porting_lib.item.CustomEnchantmentLevelItem;
import io.github.fabricators_of_create.porting_lib.item.CustomEnchantmentsItem;

public class DivingHelmetItem extends BaseArmorItem implements CustomEnchantingBehaviorItem, CustomEnchantmentLevelItem, CustomEnchantmentsItem {
	public static final EquipmentSlot SLOT = EquipmentSlot.HEAD;
	public static final ArmorItem.Type TYPE = ArmorItem.Type.HELMET;

	public DivingHelmetItem(ArmorMaterial material, Settings properties, Identifier textureLoc) {
		super(material, TYPE, properties, textureLoc);
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		if (enchantment == Enchantments.AQUA_AFFINITY) {
			return false;
		}
		return CustomEnchantingBehaviorItem.super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public int modifyEnchantmentLevel(ItemStack stack, Enchantment enchantment, int level) {
		if (enchantment == Enchantments.AQUA_AFFINITY) {
			return 1;
		}
		return level;
	}

	@Override
	public void modifyEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
		enchantments.put(Enchantments.AQUA_AFFINITY, 1);
	}

	public static boolean isWornBy(Entity entity) {
		return !getWornItem(entity).isEmpty();
	}

	public static ItemStack getWornItem(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = livingEntity.getEquippedStack(SLOT);
		if (!(stack.getItem() instanceof DivingHelmetItem)) {
			return ItemStack.EMPTY;
		}
		return stack;
	}

	public static void breatheUnderwater(LivingEntity entity) {
//		LivingEntity entity = event.getEntityLiving();
		World world = entity.getWorld();
		boolean second = world.getTime() % 20 == 0;
		boolean drowning = entity.getAir() == 0;

		if (world.isClient)
			entity.getCustomData()
				.remove("VisualBacktankAir");

		ItemStack helmet = getWornItem(entity);
		if (helmet.isEmpty())
			return;

		boolean lavaDiving = entity.isInLava();
		if (!helmet.getItem()
			.isFireproof() && lavaDiving)
			return;
		if (!entity.isSubmergedIn(AllFluidTags.DIVING_FLUIDS.tag) && !lavaDiving)
			return;
		if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative())
			return;

		List<ItemStack> backtanks = BacktankUtil.getAllWithAir(entity);
		if (backtanks.isEmpty())
			return;

		if (lavaDiving) {
			if (entity instanceof ServerPlayerEntity sp)
				AllAdvancements.DIVING_SUIT_LAVA.awardTo(sp);
			if (backtanks.stream()
				.noneMatch(backtank -> backtank.getItem()
					.isFireproof()))
				return;
		}

		if (drowning)
			entity.setAir(10);

		if (world.isClient)
			entity.getCustomData()
				.putInt("VisualBacktankAir", Math.round(backtanks.stream()
					.map(BacktankUtil::getAir)
					.reduce(0f, Float::sum)));

		if (!second)
			return;

		BacktankUtil.consumeAir(entity, backtanks.get(0), 1);

		if (lavaDiving)
			return;

		if (entity instanceof ServerPlayerEntity sp)
			AllAdvancements.DIVING_SUIT.awardTo(sp);

		entity.setAir(Math.min(entity.getMaxAir(), entity.getAir() + 10));
		entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 30, 0, true, false, true));
	}
}

package com.simibubi.create.content.equipment.armor;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.armor.CapacityEnchantment.ICapacityEnchantable;
import com.simibubi.create.foundation.item.LayeredArmorItem;

public class BacktankItem extends BaseArmorItem implements ICapacityEnchantable {
	public static final EquipmentSlot SLOT = EquipmentSlot.CHEST;
	public static final ArmorItem.Type TYPE = ArmorItem.Type.CHESTPLATE;
	public static final int BAR_COLOR = 0xEFEFEF;
	
	private final Supplier<BacktankBlockItem> blockItem;

	public BacktankItem(ArmorMaterial material, Settings properties, Identifier textureLoc, Supplier<BacktankBlockItem> placeable) {
		super(material, TYPE, properties, textureLoc);
		this.blockItem = placeable;
	}

	@Nullable
	public static BacktankItem getWornBy(Entity entity) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return null;
		}
		if (!(livingEntity.getEquippedStack(SLOT).getItem() instanceof BacktankItem item)) {
			return null;
		}
		return item;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		return blockItem.get()
			.useOnBlock(ctx);
	}

	@Override
	public boolean isDamageable() {
		return false;
	}

	@Override
	public boolean isEnchantable(ItemStack p_77616_1_) {
		return true;
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(13.0F * MathHelper.clamp(getRemainingAir(stack) / ((float) BacktankUtil.maxAir(stack)), 0, 1));
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	public Block getBlock() {
		return blockItem.get().getBlock();
	}

	public static int getRemainingAir(ItemStack stack) {
		NbtCompound orCreateTag = stack.getOrCreateNbt();
		return orCreateTag.getInt("Air");
	}

	public static class BacktankBlockItem extends BlockItem {
		private final Supplier<Item> actualItem;

		public BacktankBlockItem(Block block, Supplier<Item> actualItem, Settings properties) {
			super(block, properties);
			this.actualItem = actualItem;
		}

		@Override
		public String getTranslationKey() {
			return this.getOrCreateTranslationKey();
		}

		public Item getActualItem() {
			return actualItem.get();
		}
	}

	public static class Layered extends BacktankItem implements LayeredArmorItem {
		public Layered(ArmorMaterial material, Settings properties, Identifier textureLoc, Supplier<BacktankBlockItem> placeable) {
			super(material, properties, textureLoc, placeable);
		}

		@Override
		public String getArmorTextureLocation(LivingEntity entity, EquipmentSlot slot, ItemStack stack, int layer) {
			return String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", textureLoc.getNamespace(), textureLoc.getPath(), layer);
		}
	}
}

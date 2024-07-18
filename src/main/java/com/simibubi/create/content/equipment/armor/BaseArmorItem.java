package com.simibubi.create.content.equipment.armor;

import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import io.github.fabricators_of_create.porting_lib.item.ArmorTextureItem;

public class BaseArmorItem extends ArmorItem implements ArmorTextureItem {
	protected final Identifier textureLoc;

	public BaseArmorItem(ArmorMaterial armorMaterial, ArmorItem.Type type, Settings properties, Identifier textureLoc) {
		super(armorMaterial, type, properties.maxCount(1));
		this.textureLoc = textureLoc;
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
		return String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", textureLoc.getNamespace(), textureLoc.getPath(), slot == EquipmentSlot.LEGS ? 2 : 1, type == null ? "" : String.format(Locale.ROOT, "_%s", type));
	}
}

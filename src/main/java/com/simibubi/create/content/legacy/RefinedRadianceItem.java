package com.simibubi.create.content.legacy;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class RefinedRadianceItem extends NoGravMagicalDohickyItem {

	public RefinedRadianceItem(Settings properties) {
		super(properties);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	protected void onCreated(ItemEntity entity, NbtCompound persistentData) {
		super.onCreated(entity, persistentData);
		entity.setVelocity(entity.getVelocity()
			.add(0, .25f, 0));
	}

}

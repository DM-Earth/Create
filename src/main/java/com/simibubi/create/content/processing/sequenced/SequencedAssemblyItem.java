package com.simibubi.create.content.processing.sequenced;

import com.simibubi.create.foundation.utility.Color;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class SequencedAssemblyItem extends Item {

	public SequencedAssemblyItem(Settings p_i48487_1_) {
		super(p_i48487_1_.maxCount(1));
	}

	public float getProgress(ItemStack stack) {
		if (!stack.hasNbt())
			return 0;
		NbtCompound tag = stack.getNbt();
		if (!tag.contains("SequencedAssembly"))
			return 0;
		return tag.getCompound("SequencedAssembly")
			.getFloat("Progress");
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(getProgress(stack) * 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return Color.mixColors(0xFF_FFC074, 0xFF_46FFE0, getProgress(stack));
	}

}

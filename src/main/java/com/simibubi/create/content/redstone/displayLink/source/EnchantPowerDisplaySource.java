package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.utility.Components;

import io.github.fabricators_of_create.porting_lib.enchant.EnchantmentBonusBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class EnchantPowerDisplaySource extends NumericSingleLineDisplaySource {

	protected static final Random random = Random.create();
	protected static final ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);

	@Override
	protected MutableText provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof EnchantingTableBlockEntity))
			return ZERO.copy();

		BlockPos pos = context.getSourcePos();
		World level = context.level();
		float enchantPower = 0;

		for(BlockPos offset : EnchantingTableBlock.POWER_PROVIDER_OFFSETS) {
			if (!EnchantingTableBlock.canAccessPowerProvider(level, pos, offset))
				continue;
			BlockPos bookPos = pos.add(offset);
			BlockState state = level.getBlockState(bookPos);
			enchantPower += state.getBlock() instanceof EnchantmentBonusBlock bonus
					? bonus.getEnchantPowerBonus(state, level, pos)
					: state.isOf(Blocks.BOOKSHELF)
						? 1
						: 0;
		}


		int cost = EnchantmentHelper.calculateRequiredExperienceLevel(random, 2, (int) enchantPower, stack);

		return Components.literal(String.valueOf(cost));
	}

	@Override
	protected String getTranslationKey() {
		return "max_enchant_level";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}
}

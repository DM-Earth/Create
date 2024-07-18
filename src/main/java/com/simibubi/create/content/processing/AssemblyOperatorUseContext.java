package com.simibubi.create.content.processing;

import javax.annotation.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class AssemblyOperatorUseContext extends ItemPlacementContext {
	public AssemblyOperatorUseContext(World p_i50056_1_, @Nullable PlayerEntity p_i50056_2_, Hand p_i50056_3_, ItemStack p_i50056_4_, BlockHitResult p_i50056_5_) {
		super(p_i50056_1_, p_i50056_2_, p_i50056_3_, p_i50056_4_, p_i50056_5_);
	}
}

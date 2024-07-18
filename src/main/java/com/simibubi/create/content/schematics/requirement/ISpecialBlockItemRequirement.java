package com.simibubi.create.content.schematics.requirement;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;

public interface ISpecialBlockItemRequirement {

	public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity);

}

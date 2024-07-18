package com.simibubi.create.content.schematics.requirement;

import net.minecraft.block.BlockState;

public interface ISpecialBlockEntityItemRequirement {

	public ItemRequirement getRequiredItems(BlockState state);

}

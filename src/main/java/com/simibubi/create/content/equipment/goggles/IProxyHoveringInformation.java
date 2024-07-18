package com.simibubi.create.content.equipment.goggles;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IProxyHoveringInformation {
	
	public BlockPos getInformationSource(World level, BlockPos pos, BlockState state);

}

package com.simibubi.create.foundation.utility;

import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.state.property.Properties;

public class ColorHandlers {

	public static BlockColorProvider getGrassyBlock() {
		return (state, world, pos, layer) -> pos != null && world != null ? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getColor(0.5D, 1.0D);
	}

	public static ItemColorProvider getGrassyItem() {
		return (stack, layer) -> GrassColors.getColor(0.5D, 1.0D);
	}

	public static BlockColorProvider getRedstonePower() {
		return (state, world, pos, layer) -> RedstoneWireBlock
				.getWireColor(pos != null && world != null ? state.get(Properties.POWER) : 0);
	}

}

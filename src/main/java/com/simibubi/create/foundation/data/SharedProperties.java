package com.simibubi.create.foundation.data;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
public class SharedProperties {

	public static Block wooden() {
		return Blocks.STRIPPED_SPRUCE_WOOD;
	}

	public static Block stone() {
		return Blocks.ANDESITE;
	}

	public static Block softMetal() {
		return Blocks.GOLD_BLOCK;
	}

	public static Block copperMetal() {
		return Blocks.COPPER_BLOCK;
	}

	public static Block netheriteMetal() {
		return Blocks.NETHERITE_BLOCK;
	}
}

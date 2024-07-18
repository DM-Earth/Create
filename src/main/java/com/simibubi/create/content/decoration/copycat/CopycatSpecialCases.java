package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.content.decoration.palettes.GlassPaneBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.TrapdoorBlock;

public class CopycatSpecialCases {

	public static boolean isBarsMaterial(BlockState material) {
		return material.getBlock() instanceof PaneBlock && !(material.getBlock() instanceof GlassPaneBlock)
			&& !(material.getBlock() instanceof StainedGlassPaneBlock);
	}

	public static boolean isTrapdoorMaterial(BlockState material) {
		return material.getBlock() instanceof TrapdoorBlock && material.contains(TrapdoorBlock.HALF)
			&& material.contains(TrapdoorBlock.OPEN) && material.contains(TrapdoorBlock.FACING);
	}

}

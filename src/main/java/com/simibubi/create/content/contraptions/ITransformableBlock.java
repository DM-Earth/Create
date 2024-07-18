package com.simibubi.create.content.contraptions;

import net.minecraft.block.BlockState;

public interface ITransformableBlock {
	BlockState transform(BlockState state, StructureTransform transform);
}

package com.simibubi.create.foundation.block.render;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public interface MultiPosDestructionHandler {
	/**
	 * Returned set must be mutable and must not be changed after it is returned.
	 */
	@Nullable
	@Environment(EnvType.CLIENT)
	Set<BlockPos> getExtraPositions(ClientWorld level, BlockPos pos, BlockState blockState, int progress);
}

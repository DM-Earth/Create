package com.simibubi.create.content.decoration.copycat;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

public class FilteredBlockAndTintGetter implements BlockRenderView {

	private BlockRenderView wrapped;
	private Predicate<BlockPos> filter;

	public FilteredBlockAndTintGetter(BlockRenderView wrapped, Predicate<BlockPos> filter) {
		this.wrapped = wrapped;
		this.filter = filter;
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return filter.test(pPos) ? wrapped.getBlockEntity(pPos) : null;
	}

	@Override
	public BlockState getBlockState(BlockPos pPos) {
		return filter.test(pPos) ? wrapped.getBlockState(pPos) : Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pPos) {
		return filter.test(pPos) ? wrapped.getFluidState(pPos) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	public int getHeight() {
		return wrapped.getHeight();
	}

	@Override
	public int getBottomY() {
		return wrapped.getBottomY();
	}

	@Override
	public float getBrightness(Direction pDirection, boolean pShade) {
		return wrapped.getBrightness(pDirection, pShade);
	}

	@Override
	public LightingProvider getLightingProvider() {
		return wrapped.getLightingProvider();
	}

	@Override
	public int getColor(BlockPos pBlockPos, ColorResolver pColorResolver) {
		return wrapped.getColor(pBlockPos, pColorResolver);
	}

}

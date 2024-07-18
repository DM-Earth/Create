package com.simibubi.create.content.fluids.pipes;

import static net.minecraft.state.property.Properties.HONEY_LEVEL;

import com.simibubi.create.AllFluids;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VanillaFluidTargets {

	public static boolean shouldPipesConnectTo(BlockState state) {
		if (state.contains(Properties.HONEY_LEVEL))
			return true;
		if (state.isIn(BlockTags.CAULDRONS))
			return true;
		return false;
	}

	public static FluidStack drainBlock(World level, BlockPos pos, BlockState state, TransactionContext ctx) {
		if (state.contains(Properties.HONEY_LEVEL) && state.get(HONEY_LEVEL) >= 5) {
			level.updateSnapshots(ctx);
			level.setBlockState(pos, state.with(HONEY_LEVEL, 0), 3);
			return new FluidStack(AllFluids.HONEY.get()
				.getStill(), FluidConstants.BOTTLE);
		}

		if (state.getBlock() == Blocks.LAVA_CAULDRON) {
			level.updateSnapshots(ctx);
			level.setBlockState(pos, Blocks.CAULDRON.getDefaultState(), 3);
			return new FluidStack(Fluids.LAVA, FluidConstants.BUCKET);
		}

		if (state.getBlock() == Blocks.WATER_CAULDRON) {
			level.updateSnapshots(ctx);
			level.setBlockState(pos, Blocks.CAULDRON.getDefaultState(), 3);
			return new FluidStack(Fluids.WATER, FluidConstants.BUCKET);
		}

		return FluidStack.EMPTY;
	}

}

package com.simibubi.create.content.equipment;

import com.simibubi.create.foundation.utility.worldWrappers.PlacementSimulationServerWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PropaguleBlock;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class TreeFertilizerItem extends Item {

	public TreeFertilizerItem(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockState state = context.getWorld()
			.getBlockState(context.getBlockPos());
		Block block = state.getBlock();
		if (block instanceof Fertilizable bonemealableBlock && state.isIn(BlockTags.SAPLINGS)) {

			if (state.getOrEmpty(PropaguleBlock.HANGING)
				.orElse(false))
				return ActionResult.PASS;

			if (context.getWorld().isClient) {
				BoneMealItem.createParticles(context.getWorld(), context.getBlockPos(), 100);
				return ActionResult.SUCCESS;
			}

			BlockPos saplingPos = context.getBlockPos();
			TreesDreamWorld world = new TreesDreamWorld((ServerWorld) context.getWorld(), saplingPos);

			for (BlockPos pos : BlockPos.iterate(-1, 0, -1, 1, 0, 1)) {
				if (context.getWorld()
					.getBlockState(saplingPos.add(pos))
					.getBlock() == block)
					world.setBlockState(pos.up(10), withStage(state, 1));
			}

			bonemealableBlock.grow(world, world.getRandom(), BlockPos.ORIGIN.up(10),
					withStage(state, 1));

			for (BlockPos pos : world.blocksAdded.keySet()) {
				BlockPos actualPos = pos.add(saplingPos).down(10);
				BlockState newState = world.blocksAdded.get(pos);

				// Don't replace Bedrock
				if (context.getWorld()
					.getBlockState(actualPos)
					.getHardness(context.getWorld(), actualPos) == -1)
					continue;
				// Don't replace solid blocks with leaves
				if (!newState.isSolidBlock(world, pos)
					&& !context.getWorld()
						.getBlockState(actualPos)
						.getCollisionShape(context.getWorld(), actualPos)
						.isEmpty())
					continue;

				context.getWorld()
					.setBlockState(actualPos, newState);
			}

			if (context.getPlayer() != null && !context.getPlayer()
				.isCreative())
				context.getStack()
					.decrement(1);
			return ActionResult.SUCCESS;

		}

		return super.useOnBlock(context);
	}

	private BlockState withStage(BlockState original, int stage) {
		if (!original.contains(Properties.STAGE))
			return original;
		return original.with(Properties.STAGE, 1);
	}

	private static class TreesDreamWorld extends PlacementSimulationServerWorld {
		private final BlockState soil;

		protected TreesDreamWorld(ServerWorld wrapped, BlockPos saplingPos) {
			super(wrapped);
			BlockState stateUnderSapling = wrapped.getBlockState(saplingPos.down());
			
			// Tree features don't seem to succeed with mud as soil
			if (stateUnderSapling.isIn(BlockTags.DIRT))
				stateUnderSapling = Blocks.DIRT.getDefaultState();
			
			soil = stateUnderSapling;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			if (pos.getY() <= 9)
				return soil;
			return super.getBlockState(pos);
		}

		@Override
		public boolean setBlockState(BlockPos pos, BlockState newState, int flags) {
			if (newState.getBlock() == Blocks.PODZOL)
				return true;
			return super.setBlockState(pos, newState, flags);
		}
	}

}

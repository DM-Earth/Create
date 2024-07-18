package com.simibubi.create.content.processing;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AssemblyOperatorBlockItem extends BlockItem {

	public AssemblyOperatorBlockItem(Block block, Settings builder) {
		super(block, builder);
	}

	@Override
	public ActionResult place(ItemPlacementContext context) {
		BlockPos placedOnPos = context.getBlockPos()
			.offset(context.getSide()
				.getOpposite());
		BlockState placedOnState = context.getWorld()
			.getBlockState(placedOnPos);
		if (operatesOn(placedOnState) && context.getSide() == Direction.UP) {
			if (context.getWorld()
				.getBlockState(placedOnPos.up(2))
				.isReplaceable())
				context = adjustContext(context, placedOnPos);
			else
				return ActionResult.FAIL;
		}

		return super.place(context);
	}

	protected ItemPlacementContext adjustContext(ItemPlacementContext context, BlockPos placedOnPos) {
		BlockPos up = placedOnPos.up(2);
		return new AssemblyOperatorUseContext(context.getWorld(), context.getPlayer(), context.getHand(), context.getStack(), new BlockHitResult(new Vec3d((double)up.getX() + 0.5D + (double) Direction.UP.getOffsetX() * 0.5D, (double)up.getY() + 0.5D + (double) Direction.UP.getOffsetY() * 0.5D, (double)up.getZ() + 0.5D + (double) Direction.UP.getOffsetZ() * 0.5D), Direction.UP, up, false));
	}

	protected boolean operatesOn(BlockState placedOnState) {
		if (AllBlocks.BELT.has(placedOnState))
			return placedOnState.get(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL;
		return AllBlocks.BASIN.has(placedOnState) || AllBlocks.DEPOT.has(placedOnState) || AllBlocks.WEIGHTED_EJECTOR.has(placedOnState);
	}

}

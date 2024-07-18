package com.simibubi.create.content.kinetics.steamEngine;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class PoweredShaftBlock extends AbstractShaftBlock {

	public PoweredShaftBlock(Settings properties) {
		super(properties);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.EIGHT_VOXEL_POLE.get(pState.get(AXIS));
	}

	@Override
	public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.POWERED_SHAFT.get();
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (pPlayer.isSneaking() || !pPlayer.canModifyBlocks())
			return ActionResult.PASS;

		ItemStack heldItem = pPlayer.getStackInHand(pHand);
		IPlacementHelper helper = PlacementHelpers.get(ShaftBlock.placementHelperId);
		if (helper.matchesItem(heldItem))
			return helper.getOffset(pPlayer, pLevel, pState, pPos, pHit)
				.placeInWorld(pLevel, (BlockItem) heldItem.getItem(), pPlayer, pHand, pHit);

		return ActionResult.PASS;
	}

	@Override
	public BlockRenderType getRenderType(BlockState pState) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		if (!stillValid(pState, pLevel, pPos))
			pLevel.setBlockState(pPos, AllBlocks.SHAFT.getDefaultState()
				.with(ShaftBlock.AXIS, pState.get(AXIS))
				.with(WATERLOGGED, pState.get(WATERLOGGED)), 3);
	}

	@Override
	public ItemStack getPickStack(BlockView pLevel, BlockPos pPos, BlockState pState) {
		return AllBlocks.SHAFT.asStack();
	}

	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return stillValid(pState, pLevel, pPos);
	}

	public static boolean stillValid(BlockState pState, WorldView pLevel, BlockPos pPos) {
		for (Direction d : Iterate.directions) {
			if (d.getAxis() == pState.get(AXIS))
				continue;
			BlockPos enginePos = pPos.offset(d, 2);
			BlockState engineState = pLevel.getBlockState(enginePos);
			if (!(engineState.getBlock()instanceof SteamEngineBlock engine))
				continue;
			if (!SteamEngineBlock.getShaftPos(engineState, enginePos)
				.equals(pPos))
				continue;
			if (SteamEngineBlock.isShaftValid(engineState, pState))
				return true;
		}
		return false;
	}

	public static BlockState getEquivalent(BlockState stateForPlacement) {
		return AllBlocks.POWERED_SHAFT.getDefaultState()
			.with(PoweredShaftBlock.AXIS, stateForPlacement.get(ShaftBlock.AXIS))
			.with(WATERLOGGED, stateForPlacement.get(WATERLOGGED));
	}

}

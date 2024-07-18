package com.simibubi.create.content.contraptions.actors;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.BlockHelper;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AttachedActorBlock extends HorizontalFacingBlock
	implements IWrenchable, ProperWaterloggedBlock {

	protected AttachedActorBlock(Settings p_i48377_1_) {
		super(p_i48377_1_);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return ActionResult.FAIL;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		Direction direction = state.get(FACING);
		return AllShapes.HARVESTER_BASE.get(direction);
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
		Direction direction = state.get(FACING);
		BlockPos offset = pos.offset(direction.getOpposite());
		return BlockHelper.hasBlockSolidSide(worldIn.getBlockState(offset), worldIn, offset, direction);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction facing;
		if (context.getSide()
			.getAxis()
			.isVertical())
			facing = context.getHorizontalPlayerFacing()
				.getOpposite();
		else {
			BlockState blockState = context.getWorld()
				.getBlockState(context.getBlockPos()
					.offset(context.getSide()
						.getOpposite()));
			if (blockState.getBlock() instanceof AttachedActorBlock)
				facing = blockState.get(FACING);
			else
				facing = context.getSide();
		}
		return withWater(getDefaultState().with(FACING, facing), context);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

}

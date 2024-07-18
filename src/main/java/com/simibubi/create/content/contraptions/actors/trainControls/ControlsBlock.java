package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class ControlsBlock extends HorizontalFacingBlock implements IWrenchable, ProperWaterloggedBlock {

	public static final BooleanProperty OPEN = BooleanProperty.of("open");
	public static final BooleanProperty VIRTUAL = BooleanProperty.of("virtual");

	public ControlsBlock(Settings p_54120_) {
		super(p_54120_);
		setDefaultState(getDefaultState().with(OPEN, false)
			.with(WATERLOGGED, false)
			.with(VIRTUAL, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(FACING, OPEN, WATERLOGGED, VIRTUAL));
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState.with(OPEN, pLevel instanceof ContraptionWorld);
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		BlockState state = withWater(super.getPlacementState(pContext), pContext);
		Direction horizontalDirection = pContext.getHorizontalPlayerFacing();
		PlayerEntity player = pContext.getPlayer();

		state = state.with(FACING, horizontalDirection.getOpposite());
		if (player != null && player.isSneaking())
			state = state.with(FACING, horizontalDirection);

		return state;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return AllShapes.CONTROLS.get(pState.get(FACING));
	}

}

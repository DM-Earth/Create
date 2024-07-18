package com.simibubi.create.content.decoration;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import io.github.fabricators_of_create.porting_lib.block.CustomScaffoldingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class MetalScaffoldingBlock extends ScaffoldingBlock implements IWrenchable, CustomScaffoldingBlock {

	public MetalScaffoldingBlock(Settings pProperties) {
		super(pProperties);
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRand) {}

	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return true;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState pState, BlockView pLevel, BlockPos pPos,
		ShapeContext pContext) {
		if (pState.get(BOTTOM))
			return AllShapes.SCAFFOLD_HALF;
		return super.getCollisionShape(pState, pLevel, pPos, pContext);
	}

	@Override
	public boolean isScaffolding(BlockState state, WorldView level, BlockPos pos, LivingEntity entity) {
		return true;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		if (pState.get(BOTTOM))
			return AllShapes.SCAFFOLD_HALF;
		if (!pContext.isHolding(pState.getBlock()
			.asItem()))
			return AllShapes.SCAFFOLD_FULL;
		return VoxelShapes.fullCube();
	}

	@Override
	public VoxelShape getRaycastShape(BlockState pState, BlockView pLevel, BlockPos pPos) {
		return VoxelShapes.fullCube();
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		super.getStateForNeighborUpdate(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
		BlockState stateBelow = pLevel.getBlockState(pCurrentPos.down());
		return pFacing == Direction.DOWN ? pState.with(BOTTOM,
			!stateBelow.isOf(this) && !stateBelow.isSideSolidFullSquare(pLevel, pCurrentPos.down(), Direction.UP)) : pState;
	}

	@Override
	public boolean supportsExternalFaceHiding(BlockState state) {
		return true;
	}

	@Override
	public boolean hidesNeighborFace(BlockView level, BlockPos pos, BlockState state, BlockState neighborState,
		Direction dir) {
		if (!(neighborState.getBlock() instanceof MetalScaffoldingBlock))
			return false;
		if (!neighborState.get(BOTTOM) && state.get(BOTTOM))
			return false;
		return dir.getAxis() != Axis.Y;
	}

}

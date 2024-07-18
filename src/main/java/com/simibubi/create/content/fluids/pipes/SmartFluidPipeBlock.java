package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.TickPriority;

public class SmartFluidPipeBlock extends WallMountedBlock
	implements IBE<SmartFluidPipeBlockEntity>, IAxisPipe, IWrenchable, ProperWaterloggedBlock {

	public SmartFluidPipeBlock(Settings p_i48339_1_) {
		super(p_i48339_1_);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(FACE, FACING, WATERLOGGED);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState stateForPlacement = super.getPlacementState(ctx);
		Axis prefferedAxis = null;
		BlockPos pos = ctx.getBlockPos();
		World world = ctx.getWorld();
		for (Direction side : Iterate.directions) {
			if (!prefersConnectionTo(world, pos, side))
				continue;
			if (prefferedAxis != null && prefferedAxis != side.getAxis()) {
				prefferedAxis = null;
				break;
			}
			prefferedAxis = side.getAxis();
		}

		if (prefferedAxis == Axis.Y)
			stateForPlacement = stateForPlacement.with(FACE, WallMountLocation.WALL)
				.with(FACING, stateForPlacement.get(FACING)
					.getOpposite());
		else if (prefferedAxis != null) {
			if (stateForPlacement.get(FACE) == WallMountLocation.WALL)
				stateForPlacement = stateForPlacement.with(FACE, WallMountLocation.FLOOR);
			for (Direction direction : ctx.getPlacementDirections()) {
				if (direction.getAxis() != prefferedAxis)
					continue;
				stateForPlacement = stateForPlacement.with(FACING, direction.getOpposite());
			}
		}

		return withWater(stateForPlacement, ctx);
	}

	protected boolean prefersConnectionTo(WorldView reader, BlockPos pos, Direction facing) {
		BlockPos offset = pos.offset(facing);
		BlockState blockState = reader.getBlockState(offset);
		return FluidPipeBlock.canConnectTo(reader, offset, blockState, facing);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean blockTypeChanged = state.getBlock() != newState.getBlock();
		if (blockTypeChanged && !world.isClient)
			FluidPropagator.propagateChangedPipe(world, pos, state);
		IBE.onRemove(state, world, pos, newState);
	}

	@Override
	public boolean canPlaceAt(BlockState p_196260_1_, WorldView p_196260_2_, BlockPos p_196260_3_) {
		return true;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
		if (world.isClient)
			return;
		if (state != oldState)
			world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block otherBlock, BlockPos neighborPos,
		boolean isMoving) {
		DebugInfoSender.sendNeighborUpdate(world, pos);
		Direction d = FluidPropagator.validateNeighbourChange(state, world, pos, otherBlock, neighborPos, isMoving);
		if (d == null)
			return;
		if (!isOpenAt(state, d))
			return;
		world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);
	}

	public static boolean isOpenAt(BlockState state, Direction d) {
		return d.getAxis() == getPipeAxis(state);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random r) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
	}

	protected static Axis getPipeAxis(BlockState state) {
		return state.get(FACE) == WallMountLocation.WALL ? Axis.Y
			: state.get(FACING)
				.getAxis();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		WallMountLocation face = state.get(FACE);
		VoxelShaper shape = face == WallMountLocation.FLOOR ? AllShapes.SMART_FLUID_PIPE_FLOOR
			: face == WallMountLocation.CEILING ? AllShapes.SMART_FLUID_PIPE_CEILING : AllShapes.SMART_FLUID_PIPE_WALL;
		return shape.get(state.get(FACING));
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public Axis getAxis(BlockState state) {
		return getPipeAxis(state);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pFacing, BlockState pFacingState, WorldAccess pLevel,
		BlockPos pCurrentPos, BlockPos pFacingPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public Class<SmartFluidPipeBlockEntity> getBlockEntityClass() {
		return SmartFluidPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SmartFluidPipeBlockEntity> getBlockEntityType() {
		return  AllBlockEntityTypes.SMART_FLUID_PIPE.get();
	}

}

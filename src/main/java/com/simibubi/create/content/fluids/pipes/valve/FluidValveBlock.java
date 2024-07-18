package com.simibubi.create.content.fluids.pipes.valve;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
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
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Iterate;

public class FluidValveBlock extends DirectionalAxisKineticBlock
	implements IAxisPipe, IBE<FluidValveBlockEntity>, ProperWaterloggedBlock {

	public static final BooleanProperty ENABLED = BooleanProperty.of("enabled");

	public FluidValveBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(ENABLED, false)
			.with(WATERLOGGED, false));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.FLUID_VALVE.get(getPipeAxis(state));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(ENABLED, WATERLOGGED));
	}

	@Override
	protected boolean prefersConnectionTo(WorldView reader, BlockPos pos, Direction facing, boolean shaftAxis) {
		if (!shaftAxis) {
			BlockPos offset = pos.offset(facing);
			BlockState blockState = reader.getBlockState(offset);
			return FluidPipeBlock.canConnectTo(reader, offset, blockState, facing);
		}
		return super.prefersConnectionTo(reader, pos, facing, shaftAxis);
	}

	@Nonnull
	public static Axis getPipeAxis(BlockState state) {
		if (!(state.getBlock() instanceof FluidValveBlock))
			throw new IllegalStateException("Provided BlockState is for a different block.");
		Direction facing = state.get(FACING);
		boolean alongFirst = !state.get(AXIS_ALONG_FIRST_COORDINATE);
		for (Axis axis : Iterate.axes) {
			if (axis == facing.getAxis())
				continue;
			if (!alongFirst) {
				alongFirst = true;
				continue;
			}
			return axis;
		}
		throw new IllegalStateException("Impossible axis.");
	}

	@Override
	public Axis getAxis(BlockState state) {
		return getPipeAxis(state);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean blockTypeChanged = !state.isOf(newState.getBlock());
		if (blockTypeChanged && !world.isClient)
			FluidPropagator.propagateChangedPipe(world, pos, state);
		super.onStateReplaced(state, world, pos, newState, isMoving);
	}

	@Override
	public boolean canPlaceAt(BlockState p_196260_1_, WorldView p_196260_2_, BlockPos p_196260_3_) {
		return true;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, world, pos, oldState, isMoving);
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

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<FluidValveBlockEntity> getBlockEntityClass() {
		return FluidValveBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FLUID_VALVE.get();
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return withWater(super.getPlacementState(context), context);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		updateWater(world, state, pos);
		return state;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return fluidState(state);
	}

}

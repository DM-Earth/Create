package com.simibubi.create.content.fluids.pump;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.TickPriority;

public class PumpBlock extends DirectionalKineticBlock
	implements Waterloggable, ICogWheel, IBE<PumpBlockEntity> {

	public PumpBlock(Settings p_i48415_1_) {
		super(p_i48415_1_);
		setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		return originalState.with(FACING, originalState.get(FACING)
			.getOpposite());
	}

	@Override
	public BlockState updateAfterWrenched(BlockState newState, ItemUsageContext context) {
		return super.updateAfterWrenched(newState, context);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(FACING)
			.getAxis();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.PUMP.get(state.get(FACING));
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

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
			: Fluids.EMPTY.getDefaultState();
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(Properties.WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.get(Properties.WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		return state;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState toPlace = super.getPlacementState(context);
		World level = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		toPlace = ProperWaterloggedBlock.withWater(level, toPlace, pos);

		Direction nearestLookingDirection = context.getPlayerLookDirection();
		Direction targetDirection = context.getPlayer() != null && context.getPlayer()
			.isSneaking() ? nearestLookingDirection : nearestLookingDirection.getOpposite();
		Direction bestConnectedDirection = null;
		double bestDistance = Double.MAX_VALUE;

		for (Direction d : Iterate.directions) {
			BlockPos adjPos = pos.offset(d);
			BlockState adjState = level.getBlockState(adjPos);
			if (!FluidPipeBlock.canConnectTo(level, adjPos, adjState, d))
				continue;
			double distance = Vec3d.of(d.getVector())
				.distanceTo(Vec3d.of(targetDirection.getVector()));
			if (distance > bestDistance)
				continue;
			bestDistance = distance;
			bestConnectedDirection = d;
		}

		if (bestConnectedDirection == null)
			return toPlace;
		if (bestConnectedDirection.getAxis() == targetDirection.getAxis())
			return toPlace;
		if (player.isSneaking() && bestConnectedDirection.getAxis() != targetDirection.getAxis())
			return toPlace;

		return toPlace.with(FACING, bestConnectedDirection);
	}

	public static boolean isPump(BlockState state) {
		return state.getBlock() instanceof PumpBlock;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, world, pos, oldState, isMoving);
		if (world.isClient)
			return;
		if (state != oldState)
			world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);

		if (isPump(state) && isPump(oldState) && state.get(FACING) == oldState.get(FACING)
			.getOpposite()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!(blockEntity instanceof PumpBlockEntity))
				return;
			PumpBlockEntity pump = (PumpBlockEntity) blockEntity;
			pump.pressureUpdate = true;
		}
	}

	public static boolean isOpenAt(BlockState state, Direction d) {
		return d.getAxis() == state.get(FACING)
			.getAxis();
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random r) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean blockTypeChanged = !state.isOf(newState.getBlock());
		if (blockTypeChanged && !world.isClient)
			FluidPropagator.propagateChangedPipe(world, pos, state);
		super.onStateReplaced(state, world, pos, newState, isMoving);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<PumpBlockEntity> getBlockEntityClass() {
		return PumpBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.MECHANICAL_PUMP.get();
	}

}

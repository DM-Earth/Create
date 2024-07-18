package com.simibubi.create.content.fluids.pipes;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.TickPriority;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchableWithBracket;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Iterate;

public class FluidPipeBlock extends ConnectingBlock implements Waterloggable, IWrenchableWithBracket,
	IBE<FluidPipeBlockEntity>, EncasableBlock, ITransformableBlock {

	private static final VoxelShape OCCLUSION_BOX = Block.createCuboidShape(4, 4, 4, 12, 12, 12);

	public FluidPipeBlock(Settings properties) {
		super(4 / 16f, properties);
		this.setDefaultState(super.getDefaultState().with(Properties.WATERLOGGED, false));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		if (tryRemoveBracket(context))
			return ActionResult.SUCCESS;

		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		Direction clickedFace = context.getSide();

		Axis axis = getAxis(world, pos, state);
		if (axis == null) {
			Vec3d clickLocation = context.getHitPos()
				.subtract(pos.getX(), pos.getY(), pos.getZ());
			double closest = Float.MAX_VALUE;
			Direction argClosest = Direction.UP;
			for (Direction direction : Iterate.directions) {
				if (clickedFace.getAxis() == direction.getAxis())
					continue;
				Vec3d centerOf = Vec3d.ofCenter(direction.getVector());
				double distance = centerOf.squaredDistanceTo(clickLocation);
				if (distance < closest) {
					closest = distance;
					argClosest = direction;
				}
			}
			axis = argClosest.getAxis();
		}

		if (clickedFace.getAxis() == axis)
			return ActionResult.PASS;
		if (!world.isClient) {
			withBlockEntityDo(world, pos, fpte -> fpte.getBehaviour(FluidTransportBehaviour.TYPE).interfaces.values()
				.stream()
				.filter(pc -> pc != null && pc.hasFlow())
				.findAny()
				.ifPresent($ -> AllAdvancements.GLASS_PIPE.awardTo(context.getPlayer())));

			FluidTransportBehaviour.cacheFlows(world, pos);
			world.setBlockState(pos, AllBlocks.GLASS_FLUID_PIPE.getDefaultState()
				.with(GlassFluidPipeBlock.AXIS, axis)
				.with(Properties.WATERLOGGED, state.get(Properties.WATERLOGGED)));
			FluidTransportBehaviour.loadFlows(world, pos);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		ItemStack heldItem = player.getStackInHand(hand);
		ActionResult result = tryEncase(state, world, pos, heldItem, player, hand, ray);
		if (result.isAccepted())
			return result;

		return ActionResult.PASS;
	}

	public BlockState getAxisState(Axis axis) {
		BlockState defaultState = getDefaultState();
		for (Direction d : Iterate.directions)
			defaultState = defaultState.with(FACING_PROPERTIES.get(d), d.getAxis() == axis);
		return defaultState;
	}

	@Nullable
	private Axis getAxis(BlockView world, BlockPos pos, BlockState state) {
		return FluidPropagator.getStraightPipeAxis(state);
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		boolean blockTypeChanged = state.getBlock() != newState.getBlock();
		if (blockTypeChanged && !world.isClient)
			FluidPropagator.propagateChangedPipe(world, pos, state);
		if (state != newState && !isMoving)
			removeBracket(world, pos, true).ifPresent(stack -> Block.dropStack(world, pos, stack));
		if (state.hasBlockEntity() && (blockTypeChanged || !newState.hasBlockEntity()))
			world.removeBlockEntity(pos);
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

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random r) {
		FluidPropagator.propagateChangedPipe(world, pos, state);
	}

	public static boolean isPipe(BlockState state) {
		return state.getBlock() instanceof FluidPipeBlock;
	}

	public static boolean canConnectTo(BlockRenderView world, BlockPos neighbourPos, BlockState neighbour,
		Direction direction) {
		if (FluidPropagator.hasFluidCapability(world, neighbourPos, direction.getOpposite()))
			return true;
		if (VanillaFluidTargets.shouldPipesConnectTo(neighbour))
			return true;
		FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, neighbourPos, FluidTransportBehaviour.TYPE);
		BracketedBlockEntityBehaviour bracket =
			BlockEntityBehaviour.get(world, neighbourPos, BracketedBlockEntityBehaviour.TYPE);
		if (isPipe(neighbour))
			return bracket == null || !bracket.isBracketPresent()
				|| FluidPropagator.getStraightPipeAxis(neighbour) == direction.getAxis();
		if (transport == null)
			return false;
		return transport.canHaveFlowToward(neighbour, direction.getOpposite());
	}

	public static boolean shouldDrawRim(BlockRenderView world, BlockPos pos, BlockState state, Direction direction) {
		BlockPos offsetPos = pos.offset(direction);
		BlockState facingState = world.getBlockState(offsetPos);
		if (facingState.getBlock() instanceof EncasedPipeBlock)
			return true;
		if (!isPipe(facingState))
			return true;
		if (!canConnectTo(world, offsetPos, facingState, direction))
			return true;
		return false;
	}

	public static boolean isOpenAt(BlockState state, Direction direction) {
		return state.get(FACING_PROPERTIES.get(direction));
	}

	public static boolean isCornerOrEndPipe(BlockRenderView world, BlockPos pos, BlockState state) {
		return isPipe(state) && FluidPropagator.getStraightPipeAxis(state) == null
			&& !shouldDrawCasing(world, pos, state);
	}

	public static boolean shouldDrawCasing(BlockRenderView world, BlockPos pos, BlockState state) {
		if (!isPipe(state))
			return false;
		for (Axis axis : Iterate.axes) {
			int connections = 0;
			for (Direction direction : Iterate.directions)
				if (direction.getAxis() != axis && isOpenAt(state, direction))
					connections++;
			if (connections > 2)
				return true;
		}
		return false;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, Properties.WATERLOGGED);
		super.appendProperties(builder);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		FluidState FluidState = context.getWorld()
			.getFluidState(context.getBlockPos());
		return updateBlockState(getDefaultState(), context.getPlayerLookDirection(), null, context.getWorld(),
			context.getBlockPos()).with(Properties.WATERLOGGED,
				Boolean.valueOf(FluidState.getFluid() == Fluids.WATER));
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.get(Properties.WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (isOpenAt(state, direction) && neighbourState.contains(Properties.WATERLOGGED))
			world.scheduleBlockTick(pos, this, 1, TickPriority.HIGH);
		return updateBlockState(state, direction, direction.getOpposite(), world, pos);
	}

	public BlockState updateBlockState(BlockState state, Direction preferredDirection, @Nullable Direction ignore,
		BlockRenderView world, BlockPos pos) {

		BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
		if (bracket != null && bracket.isBracketPresent())
			return state;

		BlockState prevState = state;
		int prevStateSides = (int) Arrays.stream(Iterate.directions)
			.map(FACING_PROPERTIES::get)
			.filter(prevState::get)
			.count();

		// Update sides that are not ignored
		for (Direction d : Iterate.directions)
			if (d != ignore) {
				boolean shouldConnect = canConnectTo(world, pos.offset(d), world.getBlockState(pos.offset(d)), d);
				state = state.with(FACING_PROPERTIES.get(d), shouldConnect);
			}

		// See if it has enough connections
		Direction connectedDirection = null;
		for (Direction d : Iterate.directions) {
			if (isOpenAt(state, d)) {
				if (connectedDirection != null)
					return state;
				connectedDirection = d;
			}
		}

		// Add opposite end if only one connection
		if (connectedDirection != null)
			return state.with(FACING_PROPERTIES.get(connectedDirection.getOpposite()), true);

		// If we can't connect to anything and weren't connected before, do nothing
		if (prevStateSides == 2)
			return prevState;

		// Use preferred
		return state.with(FACING_PROPERTIES.get(preferredDirection), true)
			.with(FACING_PROPERTIES.get(preferredDirection.getOpposite()), true);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false)
			: Fluids.EMPTY.getDefaultState();
	}

	@Override
	public Optional<ItemStack> removeBracket(BlockView world, BlockPos pos, boolean inOnReplacedContext) {
		BracketedBlockEntityBehaviour behaviour =
			BracketedBlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
		if (behaviour == null)
			return Optional.empty();
		BlockState bracket = behaviour.removeBracket(inOnReplacedContext);
		if (bracket == null)
			return Optional.empty();
		return Optional.of(new ItemStack(bracket.getBlock()));
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	public Class<FluidPipeBlockEntity> getBlockEntityClass() {
		return FluidPipeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FLUID_PIPE.get();
	}

//	@Override // fabric: difficult to implement with little to gain
//	public boolean supportsExternalFaceHiding(BlockState state) {
//		return false;
//	}

	@Override
	public VoxelShape getCullingShape(BlockState pState, BlockView pLevel, BlockPos pPos) {
		return OCCLUSION_BOX;
	}

	@Override
	public BlockState rotate(BlockState pState, BlockRotation pRotation) {
		return FluidPipeBlockRotation.rotate(pState, pRotation);
	}

	@Override
	public BlockState mirror(BlockState pState, BlockMirror pMirror) {
		return FluidPipeBlockRotation.mirror(pState, pMirror);
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		return FluidPipeBlockRotation.transform(state, transform);
	}

}

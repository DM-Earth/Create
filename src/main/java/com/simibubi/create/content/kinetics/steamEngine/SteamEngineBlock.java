package com.simibubi.create.content.kinetics.steamEngine;

import static net.minecraft.state.property.Properties.WATERLOGGED;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.Couple;

public class SteamEngineBlock extends WallMountedBlock
	implements Waterloggable, IWrenchable, IBE<SteamEngineBlockEntity> {

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	public SteamEngineBlock(Settings properties) {
		super(properties);
		setDefaultState(stateManager.getDefaultState().with(FACE, WallMountLocation.FLOOR).with(FACING, Direction.NORTH).with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(FACE, FACING, WATERLOGGED));
	}

	@Override
	public void onPlaced(World pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
		super.onPlaced(pLevel, pPos, pState, pPlacer, pStack);
		AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
	}
	
	@Override
	public boolean canPlaceAt(BlockState pState, WorldView pLevel, BlockPos pPos) {
		return canPlaceAt(pLevel, pPos, getDirection(pState).getOpposite());
	}

	public static boolean canPlaceAt(WorldView pReader, BlockPos pPos, Direction pDirection) {
		BlockPos blockpos = pPos.offset(pDirection);
		return pReader.getBlockState(blockpos)
			.getBlock() instanceof FluidTankBlock;
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		ItemStack heldItem = player.getStackInHand(hand);

		IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
		if (placementHelper.matchesItem(heldItem))
			return placementHelper.getOffset(player, world, state, pos, ray)
				.placeInWorld(world, (BlockItem) heldItem.getItem(), player, hand, ray);
		return ActionResult.PASS;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbourState, WorldAccess world,
		BlockPos pos, BlockPos neighbourPos) {
		if (state.get(WATERLOGGED))
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		return state;
	}

	@Override
	public void onBlockAdded(BlockState pState, World pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
		FluidTankBlock.updateBoilerState(pState, pLevel, pPos.offset(getFacing(pState).getOpposite()));
		BlockPos shaftPos = getShaftPos(pState, pPos);
		BlockState shaftState = pLevel.getBlockState(shaftPos);
		if (isShaftValid(pState, shaftState))
			pLevel.setBlockState(shaftPos, PoweredShaftBlock.getEquivalent(shaftState), 3);
	}

	@Override
	public void onStateReplaced(BlockState pState, World pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
		if (pState.hasBlockEntity() && (!pState.isOf(pNewState.getBlock()) || !pNewState.hasBlockEntity()))
			pLevel.removeBlockEntity(pPos);
		FluidTankBlock.updateBoilerState(pState, pLevel, pPos.offset(getFacing(pState).getOpposite()));
		BlockPos shaftPos = getShaftPos(pState, pPos);
		BlockState shaftState = pLevel.getBlockState(shaftPos);
		if (AllBlocks.POWERED_SHAFT.has(shaftState))
			pLevel.scheduleBlockTick(shaftPos, shaftState.getBlock(), 1);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		WallMountLocation face = pState.get(FACE);
		Direction direction = pState.get(FACING);
		return face == WallMountLocation.CEILING ? AllShapes.STEAM_ENGINE_CEILING.get(direction.getAxis())
			: face == WallMountLocation.FLOOR ? AllShapes.STEAM_ENGINE.get(direction.getAxis())
				: AllShapes.STEAM_ENGINE_WALL.get(direction);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		World level = context.getWorld();
		BlockPos pos = context.getBlockPos();
		FluidState ifluidstate = level.getFluidState(pos);
		BlockState state = super.getPlacementState(context);
		if (state == null)
			return null;
		return state.with(WATERLOGGED, Boolean.valueOf(ifluidstate.getFluid() == Fluids.WATER));
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	public static Direction getFacing(BlockState sideState) {
		return getDirection(sideState);
	}

	public static BlockPos getShaftPos(BlockState sideState, BlockPos pos) {
		return pos.offset(getDirection(sideState), 2);
	}

	public static boolean isShaftValid(BlockState state, BlockState shaft) {
		return (AllBlocks.SHAFT.has(shaft) || AllBlocks.POWERED_SHAFT.has(shaft))
			&& shaft.get(ShaftBlock.AXIS) != getFacing(state).getAxis();
	}

	@Override
	public Class<SteamEngineBlockEntity> getBlockEntityClass() {
		return SteamEngineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STEAM_ENGINE.get();
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.SHAFT::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return s -> s.getBlock() instanceof SteamEngineBlock;
		}

		@Override
		public PlacementOffset getOffset(PlayerEntity player, World world, BlockState state, BlockPos pos,
			BlockHitResult ray) {
			BlockPos shaftPos = SteamEngineBlock.getShaftPos(state, pos);
			BlockState shaft = AllBlocks.SHAFT.getDefaultState();
			for (Direction direction : Direction.getEntityFacingOrder(player)) {
				shaft = shaft.with(ShaftBlock.AXIS, direction.getAxis());
				if (isShaftValid(state, shaft))
					break;
			}
			
			BlockState newState = world.getBlockState(shaftPos);
			if (!newState.isReplaceable())
				return PlacementOffset.fail();

			Axis axis = shaft.get(ShaftBlock.AXIS);
			return PlacementOffset.success(shaftPos,
				s -> BlockHelper.copyProperties(s, AllBlocks.POWERED_SHAFT.getDefaultState())
					.with(PoweredShaftBlock.AXIS, axis));
		}
	}
	
	public static Couple<Integer> getSpeedRange() {
		return Couple.create(16, 64);
	}

}

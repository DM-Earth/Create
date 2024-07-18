package com.simibubi.create.content.kinetics.crank;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class HandCrankBlock extends DirectionalKineticBlock
	implements IBE<HandCrankBlockEntity>, ProperWaterloggedBlock {

	public HandCrankBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.CRANK.get(state.get(FACING));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(WATERLOGGED));
	}

	public int getRotationSpeed() {
		return 32;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (player.isSpectator())
			return ActionResult.PASS;

		withBlockEntityDo(worldIn, pos, be -> be.turn(player.isSneaking()));
		if (!player.getStackInHand(handIn)
			.isOf(AllItems.EXTENDO_GRIP.get()))
			player.addExhaustion(getRotationSpeed() * AllConfigs.server().kinetics.crankHungerMultiplier.getF());

		if (player.getHungerManager()
			.getFoodLevel() == 0)
			AllAdvancements.HAND_CRANK.awardTo(player);

		return ActionResult.SUCCESS;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		Direction preferred = getPreferredFacing(context);
		BlockState defaultBlockState = withWater(getDefaultState(), context);
		if (preferred == null || (context.getPlayer() != null && context.getPlayer()
			.isSneaking()))
			return defaultBlockState.with(FACING, context.getSide());
		return defaultBlockState.with(FACING, preferred.getOpposite());
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
		Direction facing = state.get(FACING)
			.getOpposite();
		BlockPos neighbourPos = pos.offset(facing);
		BlockState neighbour = worldIn.getBlockState(neighbourPos);
		return !neighbour.getCollisionShape(worldIn, neighbourPos)
			.isEmpty();
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		if (worldIn.isClient)
			return;

		Direction blockFacing = state.get(FACING);
		if (fromPos.equals(pos.offset(blockFacing.getOpposite()))) {
			if (!canPlaceAt(state, worldIn, pos)) {
				worldIn.breakBlock(pos, true);
				return;
			}
		}
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face == state.get(FACING)
			.getOpposite();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(FACING)
			.getAxis();
	}

	@Override
	public Class<HandCrankBlockEntity> getBlockEntityClass() {
		return HandCrankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.HAND_CRANK.get();
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	public static Couple<Integer> getSpeedRange() {
		return Couple.create(32, 32);
	}

}

package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class WaterWheelBlock extends DirectionalKineticBlock implements IBE<WaterWheelBlockEntity> {

	public WaterWheelBlock(Settings properties) {
		super(properties);
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView worldIn, BlockPos pos) {
		for (Direction direction : Iterate.directions) {
			BlockPos neighbourPos = pos.offset(direction);
			BlockState neighbourState = worldIn.getBlockState(neighbourPos);
			if (!AllBlocks.WATER_WHEEL.has(neighbourState))
				continue;
			Axis axis = state.get(FACING)
				.getAxis();
			if (neighbourState.get(FACING)
				.getAxis() != axis || axis != direction.getAxis())
				return false;
		}
		return true;
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (AdventureUtil.isAdventure(pPlayer))
			return ActionResult.PASS;
		return onBlockEntityUse(pLevel, pPos, wwt -> wwt.applyMaterialIfValid(pPlayer.getStackInHand(pHand)));
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn,
		BlockPos currentPos, BlockPos facingPos) {
		if (worldIn instanceof WrappedWorld)
			return stateIn;
		if (worldIn.isClient())
			return stateIn;
		if (!worldIn.getBlockTickScheduler()
			.isQueued(currentPos, this))
			worldIn.scheduleBlockTick(currentPos, this, 1);
		return stateIn;
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
		if (worldIn.isClient())
			return;
		if (!worldIn.getBlockTickScheduler()
			.isQueued(pos, this))
			worldIn.scheduleBlockTick(pos, this, 1);
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		withBlockEntityDo(pLevel, pPos, WaterWheelBlockEntity::determineAndApplyFlowScore);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		state.with(FACING, Direction.get(AxisDirection.POSITIVE, state.get(FACING)
			.getAxis()));
		return state;
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return state.get(FACING)
			.getAxis() == face.getAxis();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(FACING)
			.getAxis();
	}

	@Override
	public float getParticleTargetRadius() {
		return 1.125f;
	}

	@Override
	public float getParticleInitialRadius() {
		return 1f;
	}

	@Override
	public boolean hideStressImpact() {
		return true;
	}

	@Override
	public Class<WaterWheelBlockEntity> getBlockEntityClass() {
		return WaterWheelBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WaterWheelBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.WATER_WHEEL.get();
	}

	public static Couple<Integer> getSpeedRange() {
		return Couple.create(8, 8);
	}

}

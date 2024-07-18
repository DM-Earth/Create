package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
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

public class LargeWaterWheelBlock extends RotatedPillarKineticBlock implements IBE<LargeWaterWheelBlockEntity> {

	public static final BooleanProperty EXTENSION = BooleanProperty.of("extension");

	public LargeWaterWheelBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(EXTENSION, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(EXTENSION));
	}

	public Axis getAxisForPlacement(ItemPlacementContext context) {
		return super.getPlacementState(context).get(AXIS);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState stateForPlacement = super.getPlacementState(context);
		BlockPos pos = context.getBlockPos();
		Axis axis = stateForPlacement.get(AXIS);

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					if (axis.choose(x, y, z) != 0)
						continue;
					BlockPos offset = new BlockPos(x, y, z);
					if (offset.equals(BlockPos.ORIGIN))
						continue;
					BlockState occupiedState = context.getWorld()
						.getBlockState(pos.add(offset));
					if (!occupiedState.isReplaceable())
						return null;
				}
			}
		}

		if (context.getWorld()
			.getBlockState(pos.offset(Direction.from(axis, AxisDirection.NEGATIVE)))
			.isOf(this))
			stateForPlacement = stateForPlacement.with(EXTENSION, true);

		return stateForPlacement;
	}

	@Override
	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		if (AdventureUtil.isAdventure(pPlayer))
			return ActionResult.PASS;
		return onBlockEntityUse(pLevel, pPos, wwt -> wwt.applyMaterialIfValid(pPlayer.getStackInHand(pHand)));
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		return ActionResult.PASS;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		if (pDirection != Direction.from(pState.get(AXIS), AxisDirection.NEGATIVE))
			return pState;
		return pState.with(EXTENSION, pNeighborState.isOf(this));
	}

	@Override
	public void onBlockAdded(BlockState state, World level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onBlockAdded(state, level, pos, oldState, isMoving);
		if (!level.getBlockTickScheduler()
			.isQueued(pos, this))
			level.scheduleBlockTick(pos, this, 1);
	}

	@Override
	public void scheduledTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		Axis axis = pState.get(AXIS);
		for (Direction side : Iterate.directions) {
			if (side.getAxis() == axis)
				continue;
			for (boolean secondary : Iterate.falseAndTrue) {
				Direction targetSide = secondary ? side.rotateClockwise(axis) : side;
				BlockPos structurePos = (secondary ? pPos.offset(side) : pPos).offset(targetSide);
				BlockState occupiedState = pLevel.getBlockState(structurePos);
				BlockState requiredStructure = AllBlocks.WATER_WHEEL_STRUCTURAL.getDefaultState()
					.with(WaterWheelStructuralBlock.FACING, targetSide.getOpposite());
				if (occupiedState == requiredStructure)
					continue;
				if (!occupiedState.isReplaceable()) {
					pLevel.breakBlock(pPos, false);
					return;
				}
				pLevel.setBlockState(structurePos, requiredStructure);
			}
		}
		withBlockEntityDo(pLevel, pPos, WaterWheelBlockEntity::determineAndApplyFlowScore);
	}

	@Override
	public BlockRenderType getRenderType(BlockState pState) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockEntityType<? extends LargeWaterWheelBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.LARGE_WATER_WHEEL.get();
	}

	@Override
	public Class<LargeWaterWheelBlockEntity> getBlockEntityClass() {
		return LargeWaterWheelBlockEntity.class;
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == getRotationAxis(state);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(AXIS);
	}

	@Override
	public float getParticleTargetRadius() {
		return 2.5f;
	}

	@Override
	public float getParticleInitialRadius() {
		return 2.25f;
	}

	public static Couple<Integer> getSpeedRange() {
		return Couple.create(4, 4);
	}

}

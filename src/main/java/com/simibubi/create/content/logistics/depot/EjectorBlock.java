package com.simibubi.create.content.logistics.depot;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity.State;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.VecHelper;
import io.github.fabricators_of_create.porting_lib.block.CustomFrictionBlock;

public class EjectorBlock extends HorizontalKineticBlock implements IBE<EjectorBlockEntity>, ProperWaterloggedBlock, CustomFrictionBlock {

	public EjectorBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(WATERLOGGED));
	}

	@Override
	public FluidState getFluidState(BlockState pState) {
		return fluidState(pState);
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState pState, Direction pDirection, BlockState pNeighborState,
		WorldAccess pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
		updateWater(pLevel, pState, pCurrentPos);
		return pState;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		return withWater(super.getPlacementState(pContext), pContext);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState p_220053_1_, BlockView p_220053_2_, BlockPos p_220053_3_,
		ShapeContext p_220053_4_) {
		return AllShapes.CASING_13PX.get(Direction.UP);
	}

	@Override
	public float getFriction(BlockState state, WorldView world, BlockPos pos, Entity entity) {
		return getBlockEntityOptional(world, pos).filter(ete -> ete.state == State.LAUNCHING)
				.map($ -> 1f)
				.orElse(getSlipperiness());
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_,
		boolean p_220069_6_) {
		withBlockEntityDo(world, pos, EjectorBlockEntity::updateSignal);
	}

	@Override
	public void onLandedUpon(World p_180658_1_, BlockState p_152427_, BlockPos p_180658_2_, Entity p_180658_3_,
		float p_180658_4_) {
		Optional<EjectorBlockEntity> blockEntityOptional = getBlockEntityOptional(p_180658_1_, p_180658_2_);
		if (blockEntityOptional.isPresent() && !p_180658_3_.bypassesLandingEffects()) {
			p_180658_3_.handleFallDamage(p_180658_4_, 1.0F, p_180658_1_.getDamageSources().fall());
			return;
		}
		super.onLandedUpon(p_180658_1_, p_152427_, p_180658_2_, p_180658_3_, p_180658_4_);
	}

	@Override
	public void onEntityLand(BlockView worldIn, Entity entityIn) {
		super.onEntityLand(worldIn, entityIn);
		BlockPos position = entityIn.getBlockPos();
		if (!AllBlocks.WEIGHTED_EJECTOR.has(worldIn.getBlockState(position)))
			return;
		if (!entityIn.isAlive())
			return;
		if (entityIn.bypassesLandingEffects())
			return;
		if (entityIn instanceof ItemEntity) {
			SharedDepotBlockMethods.onLanded(worldIn, entityIn);
			return;
		}

		Optional<EjectorBlockEntity> teProvider = getBlockEntityOptional(worldIn, position);
		if (!teProvider.isPresent())
			return;

		EjectorBlockEntity ejectorBlockEntity = teProvider.get();
		if (ejectorBlockEntity.getState() == State.RETRACTING)
			return;
		if (ejectorBlockEntity.powered)
			return;
		if (ejectorBlockEntity.launcher.getHorizontalDistance() == 0)
			return;

		if (entityIn.isOnGround()) {
			entityIn.setOnGround(false);
			Vec3d center = VecHelper.getCenterOf(position)
				.add(0, 7 / 16f, 0);
			Vec3d positionVec = entityIn.getPos();
			double diff = center.distanceTo(positionVec);
			entityIn.setVelocity(0, -0.125, 0);
			Vec3d vec = center.add(positionVec)
				.multiply(.5f);
			if (diff > 4 / 16f) {
				entityIn.setPosition(vec.x, vec.y, vec.z);
				return;
			}
		}

		ejectorBlockEntity.activate();
		ejectorBlockEntity.notifyUpdate();
		if (entityIn.getWorld().isClient)
			AllPackets.getChannel().sendToServer(new EjectorTriggerPacket(ejectorBlockEntity.getPos()));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
		BlockHitResult ray) {
		if (AllItems.WRENCH.isIn(player.getStackInHand(hand)))
			return ActionResult.PASS;
		return SharedDepotBlockMethods.onUse(state, world, pos, player, hand, ray);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.rotateYClockwise()
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return getRotationAxis(state) == face.getAxis();
	}

	@Override
	public Class<EjectorBlockEntity> getBlockEntityClass() {
		return EjectorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EjectorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.WEIGHTED_EJECTOR.get();
	}

	@Override
	public boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	public int getComparatorOutput(BlockState blockState, World worldIn, BlockPos pos) {
		return SharedDepotBlockMethods.getComparatorInputOverride(blockState, worldIn, pos);
	}

	@Override
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

}

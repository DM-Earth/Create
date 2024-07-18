package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;

import io.github.fabricators_of_create.porting_lib.block.CustomLandingEffectsBlock;
import io.github.fabricators_of_create.porting_lib.block.CustomRunningEffectsBlock;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class FakeTrackBlock extends Block implements BlockEntityProvider, ProperWaterloggedBlock, CustomLandingEffectsBlock, CustomRunningEffectsBlock {

	public FakeTrackBlock(Settings p_49795_) {
		super(p_49795_.ticksRandomly()
			.noCollision()
			.nonOpaque());
		setDefaultState(getDefaultState().with(WATERLOGGED, false));
		LandPathNodeTypesRegistry.register(this, PathNodeType.DAMAGE_OTHER, null);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState pState, BlockView pLevel, BlockPos pPos, ShapeContext pContext) {
		return VoxelShapes.empty();
	}

	@Override
	public BlockRenderType getRenderType(BlockState pState) {
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(WATERLOGGED));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		return withWater(super.getPlacementState(pContext), pContext);
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
	public void randomTick(BlockState pState, ServerWorld pLevel, BlockPos pPos, Random pRandom) {
		if (pLevel.getBlockEntity(pPos) instanceof FakeTrackBlockEntity be)
			be.randomTick();
	}

	public static void keepAlive(WorldAccess level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof FakeTrackBlockEntity be)
			be.keepAlive();
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
		return AllBlockEntityTypes.FAKE_TRACK.create(pPos, pState);
	}

	@Override
	public boolean addLandingEffects(BlockState state1, ServerWorld level, BlockPos pos, BlockState state2,
		LivingEntity entity, int numberOfParticles) {
		return true;
	}

	@Override
	public boolean addRunningEffects(BlockState state, World level, BlockPos pos, Entity entity) {
		return true;
	}

}

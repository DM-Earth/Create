package com.simibubi.create.content.logistics.funnel;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;

public abstract class AbstractFunnelBlock extends Block
	implements IBE<FunnelBlockEntity>, IWrenchable, ProperWaterloggedBlock, ReducedDestroyEffects {

	public static final BooleanProperty POWERED = Properties.POWERED;

	protected AbstractFunnelBlock(Settings p_i48377_1_) {
		super(p_i48377_1_);
		setDefaultState(getDefaultState().with(POWERED, false)
			.with(WATERLOGGED, false));
	}

//	@Environment(EnvType.CLIENT)
//	public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
//		consumer.accept(new ReducedDestroyEffects());
//	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return withWater(getDefaultState().with(POWERED, context.getWorld()
			.isReceivingRedstonePower(context.getBlockPos())), context);
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
	public boolean canPathfindThrough(BlockState state, BlockView reader, BlockPos pos, NavigationType type) {
		return false;
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		super.appendProperties(builder.add(POWERED, WATERLOGGED));
	}

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		if (worldIn.isClient)
			return;
		InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (behaviour != null)
			behaviour.onNeighborChanged(fromPos);
		if (!worldIn.getBlockTickScheduler()
			.isTicking(pos, this))
			worldIn.scheduleBlockTick(pos, this, 0);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random r) {
		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != worldIn.isReceivingRedstonePower(pos))
			worldIn.setBlockState(pos, state.cycle(POWERED), 2);
	}

	public static ItemStack tryInsert(World worldIn, BlockPos pos, ItemStack toInsert, boolean simulate) {
		FilteringBehaviour filter = BlockEntityBehaviour.get(worldIn, pos, FilteringBehaviour.TYPE);
		InvManipulationBehaviour inserter = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
		if (inserter == null)
			return toInsert;
		if (filter != null && !filter.test(toInsert))
			return toInsert;
		if (simulate)
			inserter.simulate();
		ItemStack insert = inserter.insert(toInsert);

		if (!simulate && insert.getCount() != toInsert.getCount()) {
			BlockEntity blockEntity = worldIn.getBlockEntity(pos);
			if (blockEntity instanceof FunnelBlockEntity) {
				FunnelBlockEntity funnelBlockEntity = (FunnelBlockEntity) blockEntity;
				funnelBlockEntity.onTransfer(toInsert);
				if (funnelBlockEntity.hasFlap())
					funnelBlockEntity.flap(true);
			}
		}
		return insert;
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Block block = world.getBlockState(pos.offset(getFunnelFacing(state).getOpposite()))
			.getBlock();
		return !(block instanceof AbstractFunnelBlock);
	}

	@Nullable
	public static boolean isFunnel(BlockState state) {
		return state.getBlock() instanceof AbstractFunnelBlock;
	}

	@Nullable
	public static Direction getFunnelFacing(BlockState state) {
		if (!(state.getBlock() instanceof AbstractFunnelBlock))
			return null;
		return ((AbstractFunnelBlock) state.getBlock()).getFacing(state);
	}

	protected abstract Direction getFacing(BlockState state);

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock() && !isFunnel(newState) || !newState.hasBlockEntity())
			IBE.onRemove(state, world, pos, newState);
	}

	@Override
	public Class<FunnelBlockEntity> getBlockEntityClass() {
		return FunnelBlockEntity.class;
	}

	public BlockEntityType<? extends FunnelBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.FUNNEL.get();
	};

}

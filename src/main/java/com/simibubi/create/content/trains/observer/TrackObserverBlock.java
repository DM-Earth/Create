package com.simibubi.create.content.trains.observer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class TrackObserverBlock extends Block implements IBE<TrackObserverBlockEntity>, IWrenchable, ConnectableRedstoneBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;

	public TrackObserverBlock(Settings p_49795_) {
		super(p_49795_);
		setDefaultState(getDefaultState().with(POWERED, false));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(POWERED));
	}

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side) {
		return blockState.get(POWERED) ? 15 : 0;
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, Direction side) {
		return true;
	}

	@Override
	public Class<TrackObserverBlockEntity> getBlockEntityClass() {
		return TrackObserverBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TrackObserverBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.TRACK_OBSERVER.get();
	}

	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, worldIn, pos, newState);
	}

}

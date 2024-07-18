package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.AllBlockEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class SmartChuteBlock extends AbstractChuteBlock {

	public SmartChuteBlock(Settings p_i48440_1_) {
		super(p_i48440_1_);
		setDefaultState(getDefaultState().with(POWERED, true));
	}

	public static final BooleanProperty POWERED = Properties.POWERED;

	@Override
	public void neighborUpdate(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
		boolean isMoving) {
		super.neighborUpdate(state, worldIn, pos, blockIn, fromPos, isMoving);
		if (worldIn.isClient)
			return;
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

	@Override
	public BlockState getPlacementState(ItemPlacementContext p_196258_1_) {
		return super.getPlacementState(p_196258_1_).with(POWERED, p_196258_1_.getWorld()
			.isReceivingRedstonePower(p_196258_1_.getBlockPos()));
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return true;
	}

	@Override
	public BlockEntityType<? extends ChuteBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SMART_CHUTE.get();
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> p_206840_1_) {
		super.appendProperties(p_206840_1_.add(POWERED));
	}

	@Override
	public BlockState updateChuteState(BlockState state, BlockState above, BlockView world, BlockPos pos) {
		return state;
	}

}

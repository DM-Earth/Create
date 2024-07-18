package com.simibubi.create.content.redstone.contact;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn.ColumnCoords;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.BlockHelper;

import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import io.github.fabricators_of_create.porting_lib.block.WeakPowerCheckingBlock;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneContactBlock extends WrenchableDirectionalBlock implements ConnectableRedstoneBlock, WeakPowerCheckingBlock {

	public static final BooleanProperty POWERED = Properties.POWERED;

	public RedstoneContactBlock(Settings properties) {
		super(properties);
		setDefaultState(getDefaultState().with(POWERED, false)
			.with(FACING, Direction.UP));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> builder) {
		builder.add(POWERED);
		super.appendProperties(builder);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = getDefaultState().with(FACING, context.getPlayerLookDirection()
			.getOpposite());
		Direction placeDirection = context.getSide()
			.getOpposite();

		if ((context.getPlayer() != null && context.getPlayer()
			.isSneaking()) || hasValidContact(context.getWorld(), context.getBlockPos(), placeDirection))
			state = state.with(FACING, placeDirection);
		if (hasValidContact(context.getWorld(), context.getBlockPos(), state.get(FACING)))
			state = state.with(POWERED, true);

		return state;
	}

	@Override
	public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
		ActionResult onWrenched = super.onWrenched(state, context);
		if (onWrenched != ActionResult.SUCCESS)
			return onWrenched;

		World level = context.getWorld();
		if (level.isClient())
			return onWrenched;

		BlockPos pos = context.getBlockPos();
		state = level.getBlockState(pos);
		Direction facing = state.get(RedstoneContactBlock.FACING);
		if (facing.getAxis() == Axis.Y)
			return onWrenched;
		if (ElevatorColumn.get(level, new ColumnCoords(pos.getX(), pos.getZ(), facing)) == null)
			return onWrenched;

		level.setBlockState(pos, BlockHelper.copyProperties(state, AllBlocks.ELEVATOR_CONTACT.getDefaultState()));

		return onWrenched;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState stateIn, Direction facing, BlockState facingState, WorldAccess worldIn,
		BlockPos currentPos, BlockPos facingPos) {
		if (facing != stateIn.get(FACING))
			return stateIn;
		boolean hasValidContact = hasValidContact(worldIn, currentPos, facing);
		if (stateIn.get(POWERED) != hasValidContact)
			return stateIn.with(POWERED, hasValidContact);
		return stateIn;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() == this && newState.getBlock() == this)
			if (state == newState.cycle(POWERED))
				worldIn.updateNeighborsAlways(pos, this);
		super.onStateReplaced(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
		boolean hasValidContact = hasValidContact(worldIn, pos, state.get(FACING));
		if (state.get(POWERED) != hasValidContact)
			worldIn.setBlockState(pos, state.with(POWERED, hasValidContact));
	}

	public static boolean hasValidContact(WorldAccess world, BlockPos pos, Direction direction) {
		BlockState blockState = world.getBlockState(pos.offset(direction));
		return (AllBlocks.REDSTONE_CONTACT.has(blockState) || AllBlocks.ELEVATOR_CONTACT.has(blockState))
			&& blockState.get(FACING) == direction.getOpposite();
	}

	@Override
    public boolean shouldCheckWeakPower(BlockState state, RedstoneView level, BlockPos pos, Direction side) {
        return false;
    }

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return state.get(POWERED);
	}

	@Override
	public boolean canConnectRedstone(BlockState state, BlockView world, BlockPos pos, @Nullable Direction side) {
		return side != null && state.get(FACING) != side.getOpposite();
	}

	@Override
	public int getWeakRedstonePower(BlockState state, BlockView blockAccess, BlockPos pos, Direction side) {
		return state.get(POWERED) && side != state.get(FACING)
			.getOpposite() ? 15 : 0;
	}

}

package com.simibubi.create.content.decoration;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;

public class TrainTrapdoorBlock extends TrapdoorBlock implements IWrenchable {

	public TrainTrapdoorBlock(Settings p_57526_) {
		super(p_57526_, SlidingDoorBlock.TRAIN_SET_TYPE.get());
	}

	public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand,
		BlockHitResult pHit) {
		pState = pState.cycle(OPEN);
		pLevel.setBlockState(pPos, pState, 2);
		if (pState.get(WATERLOGGED))
			pLevel.scheduleFluidTick(pPos, Fluids.WATER, Fluids.WATER.getTickRate(pLevel));
		playToggleSound(pPlayer, pLevel, pPos, pState.get(OPEN));
		return ActionResult.success(pLevel.isClient);
	}

	@Override
	public boolean isSideInvisible(BlockState state, BlockState other, Direction pDirection) {
		return state.isOf(this) == other.isOf(this) && isConnected(state, other, pDirection);
	}

	public static boolean isConnected(BlockState state, BlockState other, Direction pDirection) {
		state = state.with(WATERLOGGED, false)
			.with(POWERED, false);
		other = other.with(WATERLOGGED, false)
			.with(POWERED, false);

		boolean open = state.get(OPEN);
		BlockHalf half = state.get(HALF);
		Direction facing = state.get(FACING);

		if (open != other.get(OPEN))
			return false;
		if (!open && half == other.get(HALF))
			return pDirection.getAxis() != Axis.Y;
		if (!open && half != other.get(HALF) && pDirection.getAxis() == Axis.Y)
			return true;
		if (open && facing.getOpposite() == other.get(FACING) && pDirection.getAxis() == facing.getAxis())
			return true;
		if ((open ? state.with(HALF, BlockHalf.TOP) : state) != (open ? other.with(HALF, BlockHalf.TOP) : other))
			return false;

		return pDirection.getAxis() != facing.getAxis();
	}

}

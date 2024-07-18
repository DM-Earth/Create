package com.simibubi.create.content.equipment.bell;

import javax.annotation.Nullable;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.Attachment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;

public abstract class AbstractBellBlock<BE extends AbstractBellBlockEntity> extends BellBlock implements IBE<BE> {

	public AbstractBellBlock(Settings properties) {
		super(properties);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView reader, BlockPos pos, ShapeContext selection) {
		Direction facing = state.get(FACING);
		switch (state.get(ATTACHMENT)) {
		case CEILING:
			return AllShapes.BELL_CEILING.get(facing);
		case DOUBLE_WALL:
			return AllShapes.BELL_DOUBLE_WALL.get(facing);
		case FLOOR:
			return AllShapes.BELL_FLOOR.get(facing);
		case SINGLE_WALL:
			return AllShapes.BELL_WALL.get(facing);
		default:
			return VoxelShapes.fullCube();
		}
	}

	@Override
	public void neighborUpdate(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos,
		boolean pIsMoving) {
		if (pLevel.isClient)
			return;
		boolean shouldPower = pLevel.isReceivingRedstonePower(pPos);
		if (shouldPower == pState.get(POWERED))
			return;
		pLevel.setBlockState(pPos, pState.with(POWERED, shouldPower), 3);
		if (!shouldPower)
			return;
		Direction facing = pState.get(FACING);
		Attachment type = pState.get(ATTACHMENT);
		ring(pLevel, pPos,
			type == Attachment.CEILING || type == Attachment.FLOOR ? facing : facing.rotateYClockwise(), null);
	}

	@Override
	public boolean ring(World world, BlockState state, BlockHitResult hit, @Nullable PlayerEntity player, boolean flag) {
		BlockPos pos = hit.getBlockPos();
		Direction direction = hit.getSide();
		if (direction == null)
			direction = world.getBlockState(pos)
				.get(FACING);
		if (!this.canRingFrom(state, direction, hit.getPos().y - pos.getY()))
			return false;
		return ring(world, pos, direction, player);
	}

	protected boolean ring(World world, BlockPos pos, Direction direction, PlayerEntity player) {
		BE be = getBlockEntity(world, pos);
		if (world.isClient)
			return true;
		if (be == null || !be.ring(world, pos, direction))
			return false;
		playSound(world, pos);
		if (player != null)
			player.incrementStat(Stats.BELL_RING);
		return true;
	}

	public boolean canRingFrom(BlockState state, Direction hitDir, double heightChange) {
		if (hitDir.getAxis() == Direction.Axis.Y)
			return false;
		if (heightChange > 0.8124)
			return false;

		Direction direction = state.get(FACING);
		Attachment bellAttachment = state.get(ATTACHMENT);
		switch (bellAttachment) {
		case FLOOR:
		case CEILING:
			return direction.getAxis() == hitDir.getAxis();
		case SINGLE_WALL:
		case DOUBLE_WALL:
			return direction.getAxis() != hitDir.getAxis();
		default:
			return false;
		}
	}

	@Nullable
	public BlockEntity createBlockEntity(BlockPos p_152198_, BlockState p_152199_) {
		return IBE.super.createBlockEntity(p_152198_, p_152199_);
	}

	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World p_152194_, BlockState p_152195_,
		BlockEntityType<T> p_152196_) {
		return IBE.super.getTicker(p_152194_, p_152195_, p_152196_);
	}

	public abstract void playSound(World world, BlockPos pos);

}
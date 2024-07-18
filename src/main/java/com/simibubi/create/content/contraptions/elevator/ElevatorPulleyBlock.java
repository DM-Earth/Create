package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ElevatorPulleyBlock extends HorizontalKineticBlock implements IBE<ElevatorPulleyBlockEntity> {

	public ElevatorPulleyBlock(Settings properties) {
		super(properties);
	}

	@Override
	public ActionResult onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockHitResult hit) {
		if (!player.canModifyBlocks())
			return ActionResult.FAIL;
		if (player.isSneaking())
			return ActionResult.FAIL;
		if (!player.getStackInHand(handIn)
			.isEmpty())
			return ActionResult.PASS;
		if (worldIn.isClient)
			return ActionResult.SUCCESS;
		return onBlockEntityUse(worldIn, pos, be -> {
			be.clicked();
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public BlockEntityType<? extends ElevatorPulleyBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ELEVATOR_PULLEY.get();
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.rotateYClockwise()
			.getAxis();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, ShapeContext context) {
		return AllShapes.ELEVATOR_PULLEY.get(state.get(HORIZONTAL_FACING));
	}

	@Override
	public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
		return getRotationAxis(state) == face.getAxis();
	}

	@Override
	public Class<ElevatorPulleyBlockEntity> getBlockEntityClass() {
		return ElevatorPulleyBlockEntity.class;
	}

}

package com.simibubi.create.content.redstone;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

public class DirectedDirectionalBlock extends HorizontalFacingBlock implements IWrenchable, ITransformableBlock {

	public static final EnumProperty<WallMountLocation> TARGET = EnumProperty.of("target", WallMountLocation.class);

	public DirectedDirectionalBlock(Settings pProperties) {
		super(pProperties);
		setDefaultState(getDefaultState().with(TARGET, WallMountLocation.WALL));
	}

	@Override
	protected void appendProperties(Builder<Block, BlockState> pBuilder) {
		super.appendProperties(pBuilder.add(TARGET, FACING));
	}

	@Nullable
	public BlockState getPlacementState(ItemPlacementContext pContext) {
		for (Direction direction : pContext.getPlacementDirections()) {
			BlockState blockstate;
			if (direction.getAxis() == Direction.Axis.Y) {
				blockstate = this.getDefaultState()
					.with(TARGET, direction == Direction.UP ? WallMountLocation.CEILING : WallMountLocation.FLOOR)
					.with(FACING, pContext.getHorizontalPlayerFacing());
			} else {
				blockstate = this.getDefaultState()
					.with(TARGET, WallMountLocation.WALL)
					.with(FACING, direction.getOpposite());
			}

			return blockstate;
		}

		return null;
	}

	public static Direction getTargetDirection(BlockState pState) {
		switch ((WallMountLocation) pState.get(TARGET)) {
		case CEILING:
			return Direction.UP;
		case FLOOR:
			return Direction.DOWN;
		default:
			return pState.get(FACING);
		}
	}

	@Override
	public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
		if (targetedFace.getAxis() == Axis.Y)
			return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);

		Direction targetDirection = getTargetDirection(originalState);
		Direction newFacing = targetDirection.rotateClockwise(targetedFace.getAxis());
		if (targetedFace.getDirection() == AxisDirection.NEGATIVE)
			newFacing = newFacing.getOpposite();

		if (newFacing.getAxis() == Axis.Y)
			return originalState.with(TARGET, newFacing == Direction.UP ? WallMountLocation.CEILING : WallMountLocation.FLOOR);
		return originalState.with(TARGET, WallMountLocation.WALL)
			.with(FACING, newFacing);
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		if (transform.mirror != null)
			state = mirror(state, transform.mirror);
		if (transform.rotationAxis == Direction.Axis.Y)
			return rotate(state, transform.rotation);

		Direction targetDirection = getTargetDirection(state);
		Direction newFacing = transform.rotateFacing(targetDirection);

		if (newFacing.getAxis() == Axis.Y)
			return state.with(TARGET, newFacing == Direction.UP ? WallMountLocation.CEILING : WallMountLocation.FLOOR);
		return state.with(TARGET, WallMountLocation.WALL)
			.with(FACING, newFacing);
	}

}

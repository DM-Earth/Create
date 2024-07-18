package com.simibubi.create.content.redstone;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class FilteredDetectorFilterSlot extends ValueBoxTransform.Sided {

	private boolean hasSlotAtBottom;

	public FilteredDetectorFilterSlot(boolean hasSlotAtBottom) {
		this.hasSlotAtBottom = hasSlotAtBottom;
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		Direction targetDirection = DirectedDirectionalBlock.getTargetDirection(state);
		if (direction == targetDirection)
			return false;
		if (targetDirection.getOpposite() == direction)
			return true;

		if (targetDirection.getAxis() != Axis.Y)
			return direction == Direction.UP || direction == Direction.DOWN && hasSlotAtBottom;
		if (targetDirection == Direction.UP)
			direction = direction.getOpposite();
		if (!hasSlotAtBottom)
			return direction == state.get(DirectedDirectionalBlock.FACING);

		return direction.getAxis() == state.get(DirectedDirectionalBlock.FACING)
			.rotateYClockwise()
			.getAxis();
	}

	@Override
	public void rotate(BlockState state, MatrixStack ms) {
		super.rotate(state, ms);
		Direction facing = state.get(DirectedDirectionalBlock.FACING);
		if (facing.getAxis() == Axis.Y)
			return;
		if (getSide() != Direction.UP)
			return;
		TransformStack.cast(ms)
			.rotateZ(-AngleHelper.horizontalAngle(facing) + 180);
	}

	@Override
	protected Vec3d getSouthLocation() {
		return VecHelper.voxelSpace(8f, 8f, 15.5f);
	}

}

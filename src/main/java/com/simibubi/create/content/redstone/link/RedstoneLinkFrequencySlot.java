package com.simibubi.create.content.redstone.link;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class RedstoneLinkFrequencySlot extends ValueBoxTransform.Dual {

	public RedstoneLinkFrequencySlot(boolean first) {
		super(first);
	}

	Vec3d horizontal = VecHelper.voxelSpace(10f, 5.5f, 2.5f);
	Vec3d vertical = VecHelper.voxelSpace(10f, 2.5f, 5.5f);

	@Override
	public Vec3d getLocalOffset(BlockState state) {
		Direction facing = state.get(RedstoneLinkBlock.FACING);
		Vec3d location = VecHelper.voxelSpace(8f, 3.01f, 5.5f);

		if (facing.getAxis()
			.isHorizontal()) {
			location = VecHelper.voxelSpace(8f, 5.5f, 3.01f);
			if (isFirst())
				location = location.add(0, 5 / 16f, 0);
			return rotateHorizontally(state, location);
		}

		if (isFirst())
			location = location.add(0, 0, 5 / 16f);
		location = VecHelper.rotateCentered(location, facing == Direction.DOWN ? 180 : 0, Axis.X);
		return location;
	}

	@Override
	public void rotate(BlockState state, MatrixStack ms) {
		Direction facing = state.get(RedstoneLinkBlock.FACING);
		float yRot = facing.getAxis()
			.isVertical() ? 0 : AngleHelper.horizontalAngle(facing) + 180;
		float xRot = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
		TransformStack.cast(ms)
			.rotateY(yRot)
			.rotateX(xRot);
	}

	@Override
	public float getScale() {
		return .4975f;
	}

}

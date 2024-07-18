package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;

public class SmartChuteFilterSlotPositioning extends ValueBoxTransform.Sided {

	@Override
	public Vec3d getLocalOffset(BlockState state) {
		Direction side = getSide();
		float horizontalAngle = AngleHelper.horizontalAngle(side);
		Vec3d southLocation = VecHelper.voxelSpace(8, 11, 15.5f);
		return VecHelper.rotateCentered(southLocation, horizontalAngle, Axis.Y);
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		return direction.getAxis()
			.isHorizontal();
	}

	@Override
	protected Vec3d getSouthLocation() {
		return Vec3d.ZERO;
	}

}

package com.simibubi.create.content.kinetics.saw;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SawFilterSlot extends ValueBoxTransform {

	@Override
	public Vec3d getLocalOffset(BlockState state) {
		if (state.get(SawBlock.FACING) != Direction.UP)
			return null;
		int offset = state.get(SawBlock.FLIPPED) ? -3 : 3;
		Vec3d x = VecHelper.voxelSpace(8, 12.5f, 8 + offset);
		Vec3d z = VecHelper.voxelSpace(8 + offset, 12.5f, 8);
		return state.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? z : x;
	}

	@Override
	public void rotate(BlockState state, MatrixStack ms) {
		int yRot = (state.get(SawBlock.AXIS_ALONG_FIRST_COORDINATE) ? 90 : 0)
			+ (state.get(SawBlock.FLIPPED) ? 0 : 180);
		TransformStack.cast(ms)
			.rotateY(yRot)
			.rotateX(90);
	}

}

package com.simibubi.create.content.redstone.diodes;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;

public class BrassDiodeScrollSlot extends ValueBoxTransform {

	@Override
	public Vec3d getLocalOffset(BlockState state) {
		return VecHelper.voxelSpace(8, 2.6f, 8);
	}

	@Override
	public void rotate(BlockState state, MatrixStack ms) {
		float yRot = AngleHelper.horizontalAngle(state.get(Properties.HORIZONTAL_FACING)) + 180;
		TransformStack.cast(ms)
			.rotateY(yRot)
			.rotateX(90);
	}

	@Override
	public int getOverrideColor() {
		return 0x592424;
	}
	
}

package com.simibubi.create.content.logistics.tunnel;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.util.math.Vec3d;

public class BrassTunnelFilterSlot extends ValueBoxTransform.Sided {

	@Override
	protected Vec3d getSouthLocation() {
		return VecHelper.voxelSpace(8, 13, 15.5f);
	}

}

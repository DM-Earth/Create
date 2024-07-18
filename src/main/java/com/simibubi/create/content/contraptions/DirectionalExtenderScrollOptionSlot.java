package com.simibubi.create.content.contraptions;

import java.util.function.BiPredicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;

public class DirectionalExtenderScrollOptionSlot extends CenteredSideValueBoxTransform {

	public DirectionalExtenderScrollOptionSlot(BiPredicate<BlockState, Direction> allowedDirections) {
		super(allowedDirections);
	}

	@Override
	public Vec3d getLocalOffset(BlockState state) {
		return super.getLocalOffset(state)
				.add(Vec3d.of(state.get(Properties.FACING).getVector()).multiply(-2 / 16f));
	}

	@Override
	public void rotate(BlockState state, MatrixStack ms) {
		if (!getSide().getAxis().isHorizontal())
			TransformStack.cast(ms)
					.rotateY(AngleHelper.horizontalAngle(state.get(Properties.FACING)) + 180);
		super.rotate(state, ms);
	}
}

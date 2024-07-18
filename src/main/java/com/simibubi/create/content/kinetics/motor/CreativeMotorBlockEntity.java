package com.simibubi.create.content.kinetics.motor;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

public class CreativeMotorBlockEntity extends GeneratingKineticBlockEntity {

	public static final int DEFAULT_SPEED = 16;
	public static final int MAX_SPEED = 256;

	protected ScrollValueBehaviour generatedSpeed;

	public CreativeMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		int max = MAX_SPEED;
		generatedSpeed = new KineticScrollValueBehaviour(Lang.translateDirect("kinetics.creative_motor.rotation_speed"),
			this, new MotorValueBox());
		generatedSpeed.between(-max, max);
		generatedSpeed.value = DEFAULT_SPEED;
		generatedSpeed.withCallback(i -> this.updateGeneratedRotation());
		behaviours.add(generatedSpeed);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
			updateGeneratedRotation();
	}

	@Override
	public float getGeneratedSpeed() {
		if (!AllBlocks.CREATIVE_MOTOR.has(getCachedState()))
			return 0;
		return convertToDirection(generatedSpeed.getValue(), getCachedState().get(CreativeMotorBlock.FACING));
	}

	class MotorValueBox extends ValueBoxTransform.Sided {

		@Override
		protected Vec3d getSouthLocation() {
			return VecHelper.voxelSpace(8, 8, 12.5);
		}

		@Override
		public Vec3d getLocalOffset(BlockState state) {
			Direction facing = state.get(CreativeMotorBlock.FACING);
			return super.getLocalOffset(state).add(Vec3d.of(facing.getVector())
				.multiply(-1 / 16f));
		}

		@Override
		public void rotate(BlockState state, MatrixStack ms) {
			super.rotate(state, ms);
			Direction facing = state.get(CreativeMotorBlock.FACING);
			if (facing.getAxis() == Axis.Y)
				return;
			if (getSide() != Direction.UP)
				return;
			TransformStack.cast(ms)
				.rotateZ(-AngleHelper.horizontalAngle(facing) + 180);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			Direction facing = state.get(CreativeMotorBlock.FACING);
			if (facing.getAxis() != Axis.Y && direction == Direction.DOWN)
				return false;
			return direction.getAxis() != facing.getAxis();
		}

	}

}

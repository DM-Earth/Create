package com.simibubi.create.content.kinetics.flywheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class FlywheelBlockEntity extends KineticBlockEntity {

	LerpedFloat visualSpeed = LerpedFloat.linear();
	float angle;

	public FlywheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected Box createRenderBoundingBox() {
		return super.createRenderBoundingBox().expand(2);
	}

	@Override
	public void write(NbtCompound compound, boolean clientPacket) {
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(NbtCompound compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket)
			visualSpeed.chase(getGeneratedSpeed(), 1 / 64f, Chaser.EXP);
	}

	@Override
	public void tick() {
		super.tick();

		if (!world.isClient)
			return;

		float targetSpeed = getSpeed();
		visualSpeed.updateChaseTarget(targetSpeed);
		visualSpeed.tickChaser();
		angle += visualSpeed.getValue() * 3 / 10f;
		angle %= 360;
	}
}

package com.simibubi.create.content.kinetics.simpleRelays;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.RotationAxis;

public class BracketedKineticBlockEntityInstance extends SingleRotatingInstance<BracketedKineticBlockEntity> {

	protected RotatingData additionalShaft;

	public BracketedKineticBlockEntityInstance(MaterialManager materialManager, BracketedKineticBlockEntity blockEntity) {
		super(materialManager, blockEntity);
	}

	@Override
	public void init() {
		super.init();
		if (!ICogWheel.isLargeCog(blockEntity.getCachedState()))
			return;

		// Large cogs sometimes have to offset their teeth by 11.25 degrees in order to
		// mesh properly

		float speed = blockEntity.getSpeed();
		Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
		BlockPos pos = blockEntity.getPos();
		float offset = BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos);
		Direction facing = Direction.from(axis, AxisDirection.POSITIVE);
		Instancer<RotatingData> half = getRotatingMaterial().getModel(AllPartialModels.COGWHEEL_SHAFT, blockState,
			facing, () -> this.rotateToAxis(axis));

		additionalShaft = setup(half.createInstance(), speed);
		additionalShaft.setRotationOffset(offset);
	}

	@Override
	protected Instancer<RotatingData> getModel() {
		if (!ICogWheel.isLargeCog(blockEntity.getCachedState()))
			return super.getModel();

		Direction.Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
		Direction facing = Direction.from(axis, AxisDirection.POSITIVE);
		return getRotatingMaterial().getModel(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, blockState, facing,
			() -> this.rotateToAxis(axis));
	}

	private MatrixStack rotateToAxis(Direction.Axis axis) {
		Direction facing = Direction.from(axis, AxisDirection.POSITIVE);
		MatrixStack poseStack = new MatrixStack();
		TransformStack.cast(poseStack)
				.centre()
				.rotateToFace(facing)
				.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90))
				.unCentre();
		return poseStack;
	}

	@Override
	public void update() {
		super.update();
		if (additionalShaft != null) {
			updateRotation(additionalShaft);
			additionalShaft.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos));
		}
	}

	@Override
	public void updateLight() {
		super.updateLight();
		if (additionalShaft != null)
			relight(pos, additionalShaft);
	}

	@Override
	public void remove() {
		super.remove();
		if (additionalShaft != null)
			additionalShaft.delete();
	}

}

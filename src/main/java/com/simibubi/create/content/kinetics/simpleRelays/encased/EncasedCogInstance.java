package com.simibubi.create.content.kinetics.simpleRelays.encased;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.RotationAxis;
import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.foundation.utility.Iterate;

public class EncasedCogInstance extends KineticBlockEntityInstance<KineticBlockEntity> {

	private boolean large;

	protected RotatingData rotatingModel;
	protected Optional<RotatingData> rotatingTopShaft;
	protected Optional<RotatingData> rotatingBottomShaft;

	public static EncasedCogInstance small(MaterialManager modelManager, KineticBlockEntity blockEntity) {
		return new EncasedCogInstance(modelManager, blockEntity, false);
	}

	public static EncasedCogInstance large(MaterialManager modelManager, KineticBlockEntity blockEntity) {
		return new EncasedCogInstance(modelManager, blockEntity, true);
	}

	public EncasedCogInstance(MaterialManager modelManager, KineticBlockEntity blockEntity, boolean large) {
		super(modelManager, blockEntity);
		this.large = large;
	}

	@Override
	public void init() {
		rotatingModel = setup(getCogModel().createInstance());

		Block block = blockState.getBlock();
		if (!(block instanceof IRotate))
			return;

		IRotate def = (IRotate) block;
		rotatingTopShaft = Optional.empty();
		rotatingBottomShaft = Optional.empty();

		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!def.hasShaftTowards(blockEntity.getWorld(), blockEntity.getPos(), blockState, d))
				continue;
			RotatingData data = setup(getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, d)
				.createInstance());
			if (large)
				data.setRotationOffset(BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos));
			if (d.getDirection() == AxisDirection.POSITIVE)
				rotatingTopShaft = Optional.of(data);
			else
				rotatingBottomShaft = Optional.of(data);
		}
	}

	@Override
	public void update() {
		updateRotation(rotatingModel);
		rotatingTopShaft.ifPresent(this::updateRotation);
		rotatingBottomShaft.ifPresent(this::updateRotation);
	}

	@Override
	public void updateLight() {
		relight(pos, rotatingModel);
		rotatingTopShaft.ifPresent(d -> relight(pos, d));
		rotatingBottomShaft.ifPresent(d -> relight(pos, d));
	}

	@Override
	public void remove() {
		rotatingModel.delete();
		rotatingTopShaft.ifPresent(InstanceData::delete);
		rotatingBottomShaft.ifPresent(InstanceData::delete);
	}

	protected Instancer<RotatingData> getCogModel() {
		BlockState referenceState = blockEntity.getCachedState();
		Direction facing =
			Direction.from(referenceState.get(Properties.AXIS), AxisDirection.POSITIVE);
		PartialModel partial = large ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL : AllPartialModels.SHAFTLESS_COGWHEEL;

		return getRotatingMaterial().getModel(partial, referenceState, facing, () -> {
			MatrixStack poseStack = new MatrixStack();
			TransformStack.cast(poseStack)
				.centre()
				.rotateToFace(facing)
				.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90))
				.unCentre();
			return poseStack;
		});
	}

}

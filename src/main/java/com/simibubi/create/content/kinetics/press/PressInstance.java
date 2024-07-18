package com.simibubi.create.content.kinetics.press;

import org.joml.Quaternionf;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.util.math.RotationAxis;

public class PressInstance extends ShaftInstance<MechanicalPressBlockEntity> implements DynamicInstance {

	private final OrientedData pressHead;

	public PressInstance(MaterialManager materialManager, MechanicalPressBlockEntity blockEntity) {
		super(materialManager, blockEntity);

		pressHead = materialManager.defaultSolid()
				.material(Materials.ORIENTED)
				.getModel(AllPartialModels.MECHANICAL_PRESS_HEAD, blockState)
				.createInstance();

		Quaternionf q = RotationAxis.POSITIVE_Y
			.rotationDegrees(AngleHelper.horizontalAngle(blockState.get(MechanicalPressBlock.HORIZONTAL_FACING)));

		pressHead.setRotation(q);

		transformModels();
	}

	@Override
	public void beginFrame() {
		transformModels();
	}

	private void transformModels() {
		float renderedHeadOffset = getRenderedHeadOffset(blockEntity);

		pressHead.setPosition(getInstancePosition())
			.nudge(0, -renderedHeadOffset, 0);
	}

	private float getRenderedHeadOffset(MechanicalPressBlockEntity press) {
		PressingBehaviour pressingBehaviour = press.getPressingBehaviour();
		return pressingBehaviour.getRenderedHeadOffset(AnimationTickHolder.getPartialTicks())
			* pressingBehaviour.mode.headOffset;
	}

	@Override
	public void updateLight() {
		super.updateLight();

		relight(pos, pressHead);
	}

	@Override
	public void remove() {
		super.remove();
		pressHead.delete();
	}
}

package com.simibubi.create.content.kinetics.crafter;

import java.util.function.Supplier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;

public class ShaftlessCogwheelInstance extends SingleRotatingInstance<KineticBlockEntity> {

    public ShaftlessCogwheelInstance(MaterialManager materialManager, KineticBlockEntity blockEntity) {
        super(materialManager, blockEntity);
    }

    @Override
    protected Instancer<RotatingData> getModel() {
        Direction facing = blockState.get(MechanicalCrafterBlock.HORIZONTAL_FACING);

		return getRotatingMaterial().getModel(AllPartialModels.SHAFTLESS_COGWHEEL, blockState, facing, rotateToFace(facing));
    }

	private Supplier<MatrixStack> rotateToFace(Direction facing) {
		return () -> {
			MatrixStack stack = new MatrixStack();
			TransformStack stacker = TransformStack.cast(stack)
					.centre();

			if (facing.getAxis() == Direction.Axis.X) stacker.rotateZ(90);
			else if (facing.getAxis() == Direction.Axis.Z) stacker.rotateX(90);

			stacker.unCentre();
			return stack;
		};
	}
}

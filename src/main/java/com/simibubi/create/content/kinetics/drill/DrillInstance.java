package com.simibubi.create.content.kinetics.drill;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class DrillInstance extends SingleRotatingInstance<DrillBlockEntity> {

    public DrillInstance(MaterialManager materialManager, DrillBlockEntity blockEntity) {
        super(materialManager, blockEntity);
    }

    @Override
    protected Instancer<RotatingData> getModel() {
		BlockState referenceState = blockEntity.getCachedState();
		Direction facing = referenceState.get(Properties.FACING);
		return getRotatingMaterial().getModel(AllPartialModels.DRILL_HEAD, referenceState, facing);
	}
}

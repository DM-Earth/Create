package com.simibubi.create.content.fluids.pump;

import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class PumpCogInstance extends SingleRotatingInstance<PumpBlockEntity> implements DynamicInstance {

	public PumpCogInstance(MaterialManager materialManager, PumpBlockEntity blockEntity) {
		super(materialManager, blockEntity);
	}
	
	@Override
	public void beginFrame() {}

	@Override
	protected Instancer<RotatingData> getModel() {
		BlockState referenceState = blockEntity.getCachedState();
		Direction facing = referenceState.get(Properties.FACING);
		return getRotatingMaterial().getModel(AllPartialModels.MECHANICAL_PUMP_COG, referenceState, facing);
	}

}

package com.simibubi.create.content.kinetics.base;

import com.jozufozu.flywheel.api.MaterialManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class BackHalfShaftInstance<T extends KineticBlockEntity> extends HalfShaftInstance<T> {
    public BackHalfShaftInstance(MaterialManager materialManager, T blockEntity) {
        super(materialManager, blockEntity);
    }

    @Override
    protected Direction getShaftDirection() {
        return blockState.get(Properties.FACING).getOpposite();
    }
}

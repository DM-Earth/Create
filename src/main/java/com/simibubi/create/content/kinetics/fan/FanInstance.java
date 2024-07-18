package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.state.property.Properties.FACING;

import com.jozufozu.flywheel.api.MaterialManager;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.render.AllMaterialSpecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class FanInstance extends KineticBlockEntityInstance<EncasedFanBlockEntity> {

    protected final RotatingData shaft;
    protected final RotatingData fan;
    final Direction direction;
    private final Direction opposite;

    public FanInstance(MaterialManager materialManager, EncasedFanBlockEntity blockEntity) {
		super(materialManager, blockEntity);

		direction = blockState.get(FACING);

		opposite = direction.getOpposite();
		shaft = getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, opposite).createInstance();
		fan = materialManager.defaultCutout()
				.material(AllMaterialSpecs.ROTATING)
				.getModel(AllPartialModels.ENCASED_FAN_INNER, blockState, opposite)
				.createInstance();

		setup(shaft);
		setup(fan, getFanSpeed());
	}

    private float getFanSpeed() {
        float speed = blockEntity.getSpeed() * 5;
        if (speed > 0)
            speed = MathHelper.clamp(speed, 80, 64 * 20);
        if (speed < 0)
            speed = MathHelper.clamp(speed, -64 * 20, -80);
        return speed;
    }

    @Override
    public void update() {
        updateRotation(shaft);
        updateRotation(fan, getFanSpeed());
    }

    @Override
    public void updateLight() {
        BlockPos behind = pos.offset(opposite);
        relight(behind, shaft);

        BlockPos inFront = pos.offset(direction);
        relight(inFront, fan);
    }

    @Override
    public void remove() {
        shaft.delete();
        fan.delete();
    }
}

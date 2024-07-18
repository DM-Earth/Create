package com.simibubi.create.content.contraptions.actors.harvester;

import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class HarvesterActorInstance extends ActorInstance {
    static float originOffset = 1 / 16f;
    static Vec3d rotOffset = new Vec3d(0.5f, -2 * originOffset + 0.5f, originOffset + 0.5f);

    protected ModelData harvester;
    private Direction facing;

    protected float horizontalAngle;

    private double rotation;
    private double previousRotation;

    public HarvesterActorInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld, MovementContext context) {
        super(materialManager, simulationWorld, context);

		Material<ModelData> material = materialManager.defaultCutout()
				.material(Materials.TRANSFORMED);

        BlockState state = context.state;

        facing = state.get(Properties.HORIZONTAL_FACING);

        harvester = material.getModel(getRollingPartial(), state).createInstance();

        horizontalAngle = facing.asRotation() + ((facing.getAxis() == Direction.Axis.X) ? 180 : 0);

		harvester.setBlockLight(localBlockLight());
	}

	protected PartialModel getRollingPartial() {
		return AllPartialModels.HARVESTER_BLADE;
	}
	
	protected Vec3d getRotationOffset() {
		return rotOffset;
	}
	
	protected double getRadius() {
		return 6.5;
	}

	@Override
	public void tick() {
		super.tick();

		previousRotation = rotation;

		if (context.contraption.stalled || context.disabled
			|| VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite()))
			return;

		double arcLength = context.motion.length();

		double radians = arcLength * 16 / getRadius();

		float deg = AngleHelper.deg(radians);

		deg = (float) (((int) (deg * 3000)) / 3000);

		rotation += deg * 1.25;

		rotation %= 360;
	}

    @Override
    public void beginFrame() {
        harvester.loadIdentity()
				.translate(context.localPos)
				.centre()
				.rotateY(horizontalAngle)
				.unCentre()
				.translate(getRotationOffset())
				.rotateX(getRotation())
				.translateBack(getRotationOffset());
	}

    protected double getRotation() {
        return AngleHelper.angleLerp(AnimationTickHolder.getPartialTicks(), previousRotation, rotation);
    }
}

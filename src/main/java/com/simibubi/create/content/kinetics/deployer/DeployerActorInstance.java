package com.simibubi.create.content.kinetics.deployer;

import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.render.AllMaterialSpecs;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class DeployerActorInstance extends ActorInstance {

	private final MatrixStack stack = new MatrixStack();
	Direction facing;
    boolean stationaryTimer;

    float yRot;
    float xRot;
    float zRot;

    ModelData pole;
    ModelData hand;
    RotatingData shaft;

	public DeployerActorInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld, MovementContext context) {
        super(materialManager, simulationWorld, context);

		Material<ModelData> mat = materialManager.defaultSolid()
				.material(Materials.TRANSFORMED);

        BlockState state = context.state;
        DeployerBlockEntity.Mode mode = NBTHelper.readEnum(context.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
        PartialModel handPose = DeployerRenderer.getHandPose(mode);

        stationaryTimer = context.data.contains("StationaryTimer");
        facing = state.get(FACING);

        boolean rotatePole = state.get(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z;
        yRot = AngleHelper.horizontalAngle(facing);
        xRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
        zRot = rotatePole ? 90 : 0;

        pole = mat.getModel(AllPartialModels.DEPLOYER_POLE, state).createInstance();
        hand = mat.getModel(handPose, state).createInstance();

        Direction.Axis axis = ((IRotate) state.getBlock()).getRotationAxis(state);
        shaft = materialManager.defaultSolid()
                .material(AllMaterialSpecs.ROTATING)
				.getModel(KineticBlockEntityInstance.shaft(axis))
				.createInstance();

        int blockLight = localBlockLight();

        shaft.setRotationAxis(axis)
                .setPosition(context.localPos)
                .setBlockLight(blockLight);

        pole.setBlockLight(blockLight);
        hand.setBlockLight(blockLight);
    }

    @Override
    public void beginFrame() {
        double factor;
        if (context.disabled) {
        	factor = 0;
        } else if (context.contraption.stalled || context.position == null || context.data.contains("StationaryTimer")) {
            factor = MathHelper.sin(AnimationTickHolder.getRenderTime() * .5f) * .25f + .25f;
        } else {
        	Vec3d center = VecHelper.getCenterOf(BlockPos.ofFloored(context.position));
            double distance = context.position.distanceTo(center);
            double nextDistance = context.position.add(context.motion)
                                                  .distanceTo(center);
            factor = .5f - MathHelper.clamp(MathHelper.lerp(AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0, 1);
        }

        Vec3d offset = Vec3d.of(facing.getVector()).multiply(factor);

        TransformStack tstack = TransformStack.cast(stack);
        stack.loadIdentity();
        tstack.translate(context.localPos)
				.translate(offset);

        transformModel(stack, pole, hand, yRot, xRot, zRot);
    }

    static void transformModel(MatrixStack stack, ModelData pole, ModelData hand, float yRot, float xRot, float zRot) {
        TransformStack tstack = TransformStack.cast(stack);

        tstack.centre();
        tstack.rotate(Direction.UP, (float) ((yRot) / 180 * Math.PI));
        tstack.rotate(Direction.EAST, (float) ((xRot) / 180 * Math.PI));

        stack.push();
        tstack.rotate(Direction.SOUTH, (float) ((zRot) / 180 * Math.PI));
        tstack.unCentre();
        pole.setTransform(stack);
        stack.pop();

        tstack.unCentre();

        hand.setTransform(stack);
    }
}

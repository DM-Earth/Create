package com.simibubi.create.content.kinetics.deployer;

import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

import org.joml.Quaternionf;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.api.instance.TickableInstance;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3i;

public class DeployerInstance extends ShaftInstance<DeployerBlockEntity> implements DynamicInstance, TickableInstance {

    final Direction facing;
    final float yRot;
    final float xRot;
    final float zRot;

    protected final OrientedData pole;

    protected OrientedData hand;

    PartialModel currentHand;
    float progress;

    public DeployerInstance(MaterialManager materialManager, DeployerBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        facing = blockState.get(FACING);

        boolean rotatePole = blockState.get(AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Direction.Axis.Z;

        yRot = AngleHelper.horizontalAngle(facing);
        xRot = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
        zRot = rotatePole ? 90 : 0;

        pole = getOrientedMaterial().getModel(AllPartialModels.DEPLOYER_POLE, blockState).createInstance();

		currentHand = this.blockEntity.getHandPose();

		hand = getOrientedMaterial().getModel(currentHand, blockState).createInstance();

		progress = getProgress(AnimationTickHolder.getPartialTicks());
        updateRotation(pole, hand, yRot, xRot, zRot);
        updatePosition();
    }

    @Override
    public void tick() {
		PartialModel handPose = blockEntity.getHandPose();

		if (currentHand != handPose) {
			currentHand = handPose;
			getOrientedMaterial().getModel(currentHand, blockState)
					.stealInstance(hand);
		}
	}

    @Override
    public void beginFrame() {

        float newProgress = getProgress(AnimationTickHolder.getPartialTicks());

        if (MathHelper.approximatelyEquals(newProgress, progress)) return;

        progress = newProgress;

        updatePosition();
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, hand, pole);
    }

    @Override
    public void remove() {
        super.remove();
        hand.delete();
        pole.delete();
    }

	private float getProgress(float partialTicks) {
        if (blockEntity.state == DeployerBlockEntity.State.EXPANDING) {
			float f = 1 - (blockEntity.timer - partialTicks * blockEntity.getTimerSpeed()) / 1000f;
			if (blockEntity.fistBump)
				f *= f;
			return f;
		}
        if (blockEntity.state == DeployerBlockEntity.State.RETRACTING)
            return (blockEntity.timer - partialTicks * blockEntity.getTimerSpeed()) / 1000f;
        return 0;
    }

    private void updatePosition() {
        float handLength = currentHand == AllPartialModels.DEPLOYER_HAND_POINTING ? 0
                : currentHand == AllPartialModels.DEPLOYER_HAND_HOLDING ? 4 / 16f : 3 / 16f;
        float distance = Math.min(MathHelper.clamp(progress, 0, 1) * (blockEntity.reach + handLength), 21 / 16f);
        Vec3i facingVec = facing.getVector();
        BlockPos blockPos = getInstancePosition();

        float x = blockPos.getX() + ((float) facingVec.getX()) * distance;
        float y = blockPos.getY() + ((float) facingVec.getY()) * distance;
        float z = blockPos.getZ() + ((float) facingVec.getZ()) * distance;

        pole.setPosition(x, y, z);
        hand.setPosition(x, y, z);
    }

    static void updateRotation(OrientedData pole, OrientedData hand, float yRot, float xRot, float zRot) {

        Quaternionf q = RotationAxis.POSITIVE_Y.rotationDegrees(yRot);
        q.mul(RotationAxis.POSITIVE_X.rotationDegrees(xRot));

        hand.setRotation(q);

        q.mul(RotationAxis.POSITIVE_Z.rotationDegrees(zRot));

        pole.setRotation(q);
    }
}

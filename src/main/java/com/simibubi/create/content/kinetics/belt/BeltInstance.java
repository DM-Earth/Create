package com.simibubi.create.content.kinetics.belt;

import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.joml.Quaternionf;

import com.jozufozu.flywheel.api.InstanceData;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.BeltData;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.block.render.SpriteShiftEntry;
import com.simibubi.create.foundation.render.AllMaterialSpecs;
import com.simibubi.create.foundation.utility.Iterate;

public class BeltInstance extends KineticBlockEntityInstance<BeltBlockEntity> {

    boolean upward;
    boolean diagonal;
    boolean sideways;
    boolean vertical;
    boolean alongX;
    boolean alongZ;
    BeltSlope beltSlope;
    Direction facing;
    protected ArrayList<BeltData> keys;
    protected RotatingData pulleyKey;

    public BeltInstance(MaterialManager materialManager, BeltBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        if (!AllBlocks.BELT.has(blockState))
            return;

        keys = new ArrayList<>(2);

        beltSlope = blockState.get(BeltBlock.SLOPE);
        facing = blockState.get(BeltBlock.HORIZONTAL_FACING);
        upward = beltSlope == BeltSlope.UPWARD;
        diagonal = beltSlope.isDiagonal();
        sideways = beltSlope == BeltSlope.SIDEWAYS;
        vertical = beltSlope == BeltSlope.VERTICAL;
        alongX = facing.getAxis() == Direction.Axis.X;
        alongZ = facing.getAxis() == Direction.Axis.Z;

        BeltPart part = blockState.get(BeltBlock.PART);
        boolean start = part == BeltPart.START;
        boolean end = part == BeltPart.END;
        DyeColor color = blockEntity.color.orElse(null);

        for (boolean bottom : Iterate.trueAndFalse) {
            PartialModel beltPartial = BeltRenderer.getBeltPartial(diagonal, start, end, bottom);
            SpriteShiftEntry spriteShift = BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);

            Instancer<BeltData> beltModel = materialManager.defaultSolid()
                    .material(AllMaterialSpecs.BELTS)
                    .getModel(beltPartial, blockState);

            keys.add(setup(beltModel.createInstance(), bottom, spriteShift));

            if (diagonal) break;
        }

        if (blockEntity.hasPulley()) {
            Instancer<RotatingData> pulleyModel = getPulleyModel();

            pulleyKey = setup(pulleyModel.createInstance());
        }
    }

    @Override
    public void update() {
        DyeColor color = blockEntity.color.orElse(null);

        boolean bottom = true;
        for (BeltData key : keys) {

            SpriteShiftEntry spriteShiftEntry = BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);
            key.setScrollTexture(spriteShiftEntry)
               .setColor(blockEntity)
               .setRotationalSpeed(getScrollSpeed());
            bottom = false;
        }

        if (pulleyKey != null) {
            updateRotation(pulleyKey);
        }
    }

    @Override
    public void updateLight() {
        relight(pos, keys.stream());

        if (pulleyKey != null) relight(pos, pulleyKey);
    }

    @Override
    public void remove() {
        keys.forEach(InstanceData::delete);
        keys.clear();
        if (pulleyKey != null) pulleyKey.delete();
        pulleyKey = null;
    }

    private float getScrollSpeed() {
        float speed = blockEntity.getSpeed();
        if (((facing.getDirection() == Direction.AxisDirection.NEGATIVE) ^ upward) ^
                ((alongX && !diagonal) || (alongZ && diagonal))) {
            speed = -speed;
        }
        if (sideways && (facing == Direction.SOUTH || facing == Direction.WEST) || (vertical && facing == Direction.EAST))
            speed = -speed;

        return speed;
    }

    private Instancer<RotatingData> getPulleyModel() {
        Direction dir = getOrientation();

        Direction.Axis axis = dir.getAxis();

        Supplier<MatrixStack> ms = () -> {
            MatrixStack modelTransform = new MatrixStack();
            TransformStack msr = TransformStack.cast(modelTransform);
            msr.centre();
            if (axis == Direction.Axis.X)
                msr.rotateY(90);
            if (axis == Direction.Axis.Y)
                msr.rotateX(90);
            msr.rotateX(90);
            msr.unCentre();

            return modelTransform;
        };

        return getRotatingMaterial().getModel(AllPartialModels.BELT_PULLEY, blockState, dir, ms);
    }

    private Direction getOrientation() {
        Direction dir = blockState.get(BeltBlock.HORIZONTAL_FACING)
                                  .rotateYClockwise();
        if (beltSlope == BeltSlope.SIDEWAYS)
            dir = Direction.UP;

        return dir;
    }

    private BeltData setup(BeltData key, boolean bottom, SpriteShiftEntry spriteShift) {
        boolean downward = beltSlope == BeltSlope.DOWNWARD;
        float rotX = (!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0) + (downward ? 180 : 0) + (sideways ? 90 : 0) + (vertical && alongZ ? 180 : 0);
        float rotY = facing.asRotation() + ((diagonal ^ alongX) && !downward ? 180 : 0) + (sideways && alongZ ? 180 : 0) + (vertical && alongX ? 90 : 0);
        float rotZ = (sideways ? 90 : 0) + (vertical && alongX ? 90 : 0);

        Quaternionf q = new Quaternionf().rotationXYZ(rotX * MathHelper.RADIANS_PER_DEGREE, rotY * MathHelper.RADIANS_PER_DEGREE, rotZ * MathHelper.RADIANS_PER_DEGREE);

		key.setScrollTexture(spriteShift)
				.setScrollMult(diagonal ? 3f / 8f : 0.5f)
				.setRotation(q)
				.setRotationalSpeed(getScrollSpeed())
				.setRotationOffset(bottom ? 0.5f : 0f)
                .setColor(blockEntity)
                .setPosition(getInstancePosition())
                .setBlockLight(world.getLightLevel(LightType.BLOCK, pos))
                .setSkyLight(world.getLightLevel(LightType.SKY, pos));

        return key;
    }

}

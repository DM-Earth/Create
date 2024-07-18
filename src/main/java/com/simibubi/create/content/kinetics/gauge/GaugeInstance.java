package com.simibubi.create.content.kinetics.gauge;

import java.util.ArrayList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import com.jozufozu.flywheel.api.Instancer;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;

public abstract class GaugeInstance extends ShaftInstance<GaugeBlockEntity> implements DynamicInstance {

    protected final ArrayList<DialFace> faces;

    protected MatrixStack ms;

    protected GaugeInstance(MaterialManager materialManager, GaugeBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        faces = new ArrayList<>(2);

        GaugeBlock gaugeBlock = (GaugeBlock) blockState.getBlock();

        Instancer<ModelData> dialModel = getTransformMaterial().getModel(AllPartialModels.GAUGE_DIAL, blockState);
        Instancer<ModelData> headModel = getHeadModel();

        ms = new MatrixStack();
        TransformStack msr = TransformStack.cast(ms);
        msr.translate(getInstancePosition());

        float progress = MathHelper.lerp(AnimationTickHolder.getPartialTicks(), blockEntity.prevDialState, blockEntity.dialState);

        for (Direction facing : Iterate.directions) {
            if (!gaugeBlock.shouldRenderHeadOnFace(world, pos, blockState, facing))
                continue;

            DialFace face = makeFace(facing, dialModel, headModel);

            faces.add(face);

            face.setupTransform(msr, progress);
        }
    }

    private DialFace makeFace(Direction face, Instancer<ModelData> dialModel, Instancer<ModelData> headModel) {
        return new DialFace(face, dialModel.createInstance(), headModel.createInstance());
    }

    @Override
    public void beginFrame() {
        GaugeBlockEntity gaugeBlockEntity = (GaugeBlockEntity) blockEntity;

        if (MathHelper.approximatelyEquals(gaugeBlockEntity.prevDialState, gaugeBlockEntity.dialState))
            return;

        float progress = MathHelper.lerp(AnimationTickHolder.getPartialTicks(), gaugeBlockEntity.prevDialState, gaugeBlockEntity.dialState);

        TransformStack msr = TransformStack.cast(ms);

        for (DialFace faceEntry : faces) {
            faceEntry.updateTransform(msr, progress);
        }
    }

    @Override
    public void updateLight() {
        super.updateLight();

        relight(pos, faces.stream()
                          .flatMap(Couple::stream));
    }

    @Override
    public void remove() {
        super.remove();

        faces.forEach(DialFace::delete);
    }

    protected abstract Instancer<ModelData> getHeadModel();

    private class DialFace extends Couple<ModelData> {

        Direction face;

        public DialFace(Direction face, ModelData first, ModelData second) {
            super(first, second);
            this.face = face;
        }

        private void setupTransform(TransformStack msr, float progress) {
            float dialPivot = 5.75f / 16;

            msr.pushPose();
            rotateToFace(msr);

            getSecond().setTransform(ms);

            msr.translate(0, dialPivot, dialPivot)
               .rotate(Direction.EAST, (float) (Math.PI / 2 * -progress))
               .translate(0, -dialPivot, -dialPivot);

            getFirst().setTransform(ms);

            msr.popPose();
        }

        private void updateTransform(TransformStack msr, float progress) {
            float dialPivot = 5.75f / 16;

            msr.pushPose();

            rotateToFace(msr)
                    .translate(0, dialPivot, dialPivot)
                    .rotate(Direction.EAST, (float) (Math.PI / 2 * -progress))
                    .translate(0, -dialPivot, -dialPivot);

            getFirst().setTransform(ms);

            msr.popPose();
        }

        protected TransformStack rotateToFace(TransformStack msr) {
            return msr.centre()
                      .rotate(Direction.UP, (float) ((-face.asRotation() - 90) / 180 * Math.PI))
                      .unCentre();
        }

        private void delete() {
            getFirst().delete();
            getSecond().delete();
        }
    }

    public static class Speed extends GaugeInstance {
        public Speed(MaterialManager materialManager, GaugeBlockEntity blockEntity) {
            super(materialManager, blockEntity);
        }

        @Override
        protected Instancer<ModelData> getHeadModel() {
            return getTransformMaterial().getModel(AllPartialModels.GAUGE_HEAD_SPEED, blockState);
        }
    }

    public static class Stress extends GaugeInstance {
        public Stress(MaterialManager materialManager, GaugeBlockEntity blockEntity) {
            super(materialManager, blockEntity);
        }

        @Override
        protected Instancer<ModelData> getHeadModel() {
            return getTransformMaterial().getModel(AllPartialModels.GAUGE_HEAD_STRESS, blockState);
        }
    }
}

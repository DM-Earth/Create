package com.simibubi.create.content.kinetics.mixer;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class MechanicalMixerRenderer extends KineticBlockEntityRenderer<MechanicalMixerBlockEntity> {

	public MechanicalMixerRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean rendersOutsideBoundingBox(MechanicalMixerBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalMixerBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

		if (Backend.canUseInstancing(be.getWorld())) return;

		BlockState blockState = be.getCachedState();

		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());

		SuperByteBuffer superBuffer = CachedBufferer.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);

		float renderedHeadOffset = be.getRenderedHeadOffset(partialTicks);
		float speed = be.getRenderedHeadRotationSpeed(partialTicks);
		float time = AnimationTickHolder.getRenderTime(be.getWorld());
		float angle = ((time * speed * 6 / 10f) % 360) / 180 * (float) Math.PI;

		SuperByteBuffer poleRender = CachedBufferer.partial(AllPartialModels.MECHANICAL_MIXER_POLE, blockState);
		poleRender.translate(0, -renderedHeadOffset, 0)
				.light(light)
				.renderInto(ms, vb);

		VertexConsumer vbCutout = buffer.getBuffer(RenderLayer.getCutoutMipped());
		SuperByteBuffer headRender = CachedBufferer.partial(AllPartialModels.MECHANICAL_MIXER_HEAD, blockState);
		headRender.rotateCentered(Direction.UP, angle)
				.translate(0, -renderedHeadOffset, 0)
				.light(light)
				.renderInto(ms, vbCutout);
	}

}

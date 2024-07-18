package com.simibubi.create.content.kinetics.press;

import static net.minecraft.state.property.Properties.HORIZONTAL_FACING;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class MechanicalPressRenderer extends KineticBlockEntityRenderer<MechanicalPressBlockEntity> {

	public MechanicalPressRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public boolean rendersOutsideBoundingBox(MechanicalPressBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalPressBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		BlockState blockState = be.getCachedState();
		PressingBehaviour pressingBehaviour = be.getPressingBehaviour();
		float renderedHeadOffset =
			pressingBehaviour.getRenderedHeadOffset(partialTicks) * pressingBehaviour.mode.headOffset;

		SuperByteBuffer headRender = CachedBufferer.partialFacing(AllPartialModels.MECHANICAL_PRESS_HEAD, blockState,
			blockState.get(HORIZONTAL_FACING));
		headRender.translate(0, -renderedHeadOffset, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
	}

	@Override
	protected BlockState getRenderedBlockState(MechanicalPressBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}

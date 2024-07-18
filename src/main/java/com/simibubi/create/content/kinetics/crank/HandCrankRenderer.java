package com.simibubi.create.content.kinetics.crank;

import static net.minecraft.state.property.Properties.FACING;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class HandCrankRenderer extends KineticBlockEntityRenderer<HandCrankBlockEntity> {

	public HandCrankRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(HandCrankBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (be.shouldRenderShaft())
			super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		Direction facing = be.getCachedState()
			.get(FACING);
		kineticRotationTransform(be.getRenderedHandle(), be, facing.getAxis(), be.getIndependentAngle(partialTicks),
			light).renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
	}

}

package com.simibubi.create.content.redstone.displayLink;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.AngleHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class DisplayLinkRenderer extends SafeBlockEntityRenderer<DisplayLinkBlockEntity> {

	public DisplayLinkRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(DisplayLinkBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		float glow = be.glow.getValue(partialTicks);
		if (glow < .125f)
			return;

		glow = (float) (1 - (2 * Math.pow(glow - .75f, 2)));
		glow = MathHelper.clamp(glow, -1, 1);

		int color = (int) (200 * glow);

		BlockState blockState = be.getCachedState();
		TransformStack msr = TransformStack.cast(ms);

		Direction face = blockState.getOrEmpty(DisplayLinkBlock.FACING)
			.orElse(Direction.UP);

		if (face.getAxis()
			.isHorizontal())
			face = face.getOpposite();

		ms.push();

		msr.centre()
			.rotateY(AngleHelper.horizontalAngle(face))
			.rotateX(-AngleHelper.verticalAngle(face) - 90)
			.unCentre();

		CachedBufferer.partial(AllPartialModels.DISPLAY_LINK_TUBE, blockState)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getTranslucent()));

		CachedBufferer.partial(AllPartialModels.DISPLAY_LINK_GLOW, blockState)
			.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
			.color(color, color, color, 255)
			.disableDiffuse()
			.renderInto(ms, buffer.getBuffer(RenderTypes.getAdditive()));

		ms.pop();
	}

}

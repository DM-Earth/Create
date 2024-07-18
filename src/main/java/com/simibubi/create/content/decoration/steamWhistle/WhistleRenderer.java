package com.simibubi.create.content.decoration.steamWhistle;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class WhistleRenderer extends SafeBlockEntityRenderer<WhistleBlockEntity> {

	public WhistleRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(WhistleBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		if (!(blockState.getBlock() instanceof WhistleBlock))
			return;

		Direction direction = blockState.get(WhistleBlock.FACING);
		WhistleSize size = blockState.get(WhistleBlock.SIZE);

		PartialModel mouth = size == WhistleSize.LARGE ? AllPartialModels.WHISTLE_MOUTH_LARGE
			: size == WhistleSize.MEDIUM ? AllPartialModels.WHISTLE_MOUTH_MEDIUM : AllPartialModels.WHISTLE_MOUTH_SMALL;

		float offset = be.animation.getValue(partialTicks);
		if (be.animation.getChaseTarget() > 0 && be.animation.getValue() > 0.5f) {
			float wiggleProgress = (AnimationTickHolder.getTicks(be.getWorld()) + partialTicks) / 8f;
			offset -= Math.sin(wiggleProgress * (2 * MathHelper.PI) * (4 - size.ordinal())) / 16f;
		}

		CachedBufferer.partial(mouth, blockState)
			.centre()
			.rotateY(AngleHelper.horizontalAngle(direction))
			.unCentre()
			.translate(0, offset * 4 / 16f, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
	}

}

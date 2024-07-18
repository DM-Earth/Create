package com.simibubi.create.content.trains.signal;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.OverlayState;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SignalRenderer extends SafeBlockEntityRenderer<SignalBlockEntity> {

	public SignalRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(SignalBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		BlockState blockState = be.getCachedState();
		SignalState signalState = be.getState();
		OverlayState overlayState = be.getOverlay();

		float renderTime = AnimationTickHolder.getRenderTime(be.getWorld());
		if (signalState.isRedLight(renderTime))
			CachedBufferer.partial(AllPartialModels.SIGNAL_ON, blockState)
				.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
		else
			CachedBufferer.partial(AllPartialModels.SIGNAL_OFF, blockState)
				.light(light)
				.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));

		BlockPos pos = be.getPos();
		TrackTargetingBehaviour<SignalBoundary> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		World level = be.getWorld();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;
		if (overlayState == OverlayState.SKIP)
			return;

		ms.push();
		TransformStack.cast(ms)
			.translate(targetPosition.subtract(pos));
		RenderedTrackOverlayType type =
			overlayState == OverlayState.DUAL ? RenderedTrackOverlayType.DUAL_SIGNAL : RenderedTrackOverlayType.SIGNAL;
		TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			buffer, light, overlay, type, 1);
		ms.pop();

	}

}

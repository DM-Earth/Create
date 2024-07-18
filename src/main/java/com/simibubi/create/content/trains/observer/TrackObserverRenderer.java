package com.simibubi.create.content.trains.observer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrackObserverRenderer extends SmartBlockEntityRenderer<TrackObserverBlockEntity> {

	public TrackObserverRenderer(Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TrackObserverBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		BlockPos pos = be.getPos();

		TrackTargetingBehaviour<TrackObserver> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		World level = be.getWorld();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;

		ms.push();
		TransformStack.cast(ms)
			.translate(targetPosition.subtract(pos));
		RenderedTrackOverlayType type = RenderedTrackOverlayType.OBSERVER;
		TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			buffer, light, overlay, type, 1);
		ms.pop();

	}

}

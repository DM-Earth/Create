package com.simibubi.create.content.kinetics.gauge;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock.Type;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class GaugeRenderer extends ShaftRenderer<GaugeBlockEntity> {

	protected GaugeBlock.Type type;

	public static GaugeRenderer speed(BlockEntityRendererFactory.Context context) {
		return new GaugeRenderer(context, Type.SPEED);
	}

	public static GaugeRenderer stress(BlockEntityRendererFactory.Context context) {
		return new GaugeRenderer(context, Type.STRESS);
	}

	protected GaugeRenderer(BlockEntityRendererFactory.Context context, GaugeBlock.Type type) {
		super(context);
		this.type = type;
	}

	@Override
	protected void renderSafe(GaugeBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (Backend.canUseInstancing(be.getWorld())) return;

		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		BlockState gaugeState = be.getCachedState();
		GaugeBlockEntity gaugeBE = (GaugeBlockEntity) be;

		PartialModel partialModel = (type == Type.SPEED ? AllPartialModels.GAUGE_HEAD_SPEED : AllPartialModels.GAUGE_HEAD_STRESS);
		SuperByteBuffer headBuffer =
				CachedBufferer.partial(partialModel, gaugeState);
		SuperByteBuffer dialBuffer = CachedBufferer.partial(AllPartialModels.GAUGE_DIAL, gaugeState);

		float dialPivot = 5.75f / 16;
		float progress = MathHelper.lerp(partialTicks, gaugeBE.prevDialState, gaugeBE.dialState);

		for (Direction facing : Iterate.directions) {
			if (!((GaugeBlock) gaugeState.getBlock()).shouldRenderHeadOnFace(be.getWorld(), be.getPos(), gaugeState,
					facing))
				continue;

			VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
			rotateBufferTowards(dialBuffer, facing).translate(0, dialPivot, dialPivot)
				.rotate(Direction.EAST, (float) (Math.PI / 2 * -progress))
				.translate(0, -dialPivot, -dialPivot)
				.light(light)
				.renderInto(ms, vb);
			rotateBufferTowards(headBuffer, facing).light(light)
				.renderInto(ms, vb);
		}
	}

	protected SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
		return buffer.rotateCentered(Direction.UP, (float) ((-target.asRotation() - 90) / 180 * Math.PI));
	}

}

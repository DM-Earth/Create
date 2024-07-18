package com.simibubi.create.content.logistics.funnel;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FunnelRenderer extends SmartBlockEntityRenderer<FunnelBlockEntity> {

	public FunnelRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FunnelBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (!be.hasFlap() || Backend.canUseInstancing(be.getWorld()))
			return;

		BlockState blockState = be.getCachedState();
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		PartialModel partialModel = (blockState.getBlock() instanceof FunnelBlock ? AllPartialModels.FUNNEL_FLAP
			: AllPartialModels.BELT_FUNNEL_FLAP);
		SuperByteBuffer flapBuffer = CachedBufferer.partial(partialModel, blockState);
		Vec3d pivot = VecHelper.voxelSpace(0, 10, 9.5f);
		TransformStack msr = TransformStack.cast(ms);

		float horizontalAngle = AngleHelper.horizontalAngle(FunnelBlock.getFunnelFacing(blockState)
			.getOpposite());
		float f = be.flap.getValue(partialTicks);

		ms.push();
		msr.centre()
			.rotateY(horizontalAngle)
			.unCentre();
		ms.translate(0.075f / 16f, 0, -be.getFlapOffset());

		for (int segment = 0; segment <= 3; segment++) {
			ms.push();

			float intensity = segment == 3 ? 1.5f : segment + 1;
			float abs = Math.abs(f);
			float flapAngle = MathHelper.sin((float) ((1 - abs) * Math.PI * intensity)) * 30 * -f;
			if (f > 0)
				flapAngle *= .5f;

			msr.translate(pivot)
				.rotateX(flapAngle)
				.translateBack(pivot);

			flapBuffer.light(light)
				.renderInto(ms, vb);

			ms.pop();
			ms.translate(-3.05f / 16f, 0, 0);
		}
		ms.pop();
	}

}

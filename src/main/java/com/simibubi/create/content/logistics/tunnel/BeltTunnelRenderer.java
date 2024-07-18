package com.simibubi.create.content.logistics.tunnel;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BeltTunnelRenderer extends SmartBlockEntityRenderer<BeltTunnelBlockEntity> {

	public BeltTunnelRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BeltTunnelBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (Backend.canUseInstancing(be.getWorld()))
			return;

		SuperByteBuffer flapBuffer = CachedBufferer.partial(AllPartialModels.BELT_TUNNEL_FLAP, be.getCachedState());
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
		Vec3d pivot = VecHelper.voxelSpace(0, 10, 1f);
		TransformStack msr = TransformStack.cast(ms);

		for (Direction direction : Iterate.directions) {
			if (!be.flaps.containsKey(direction))
				continue;

			float horizontalAngle = AngleHelper.horizontalAngle(direction.getOpposite());
			float f = be.flaps.get(direction)
				.getValue(partialTicks);

			ms.push();
			msr.centre()
				.rotateY(horizontalAngle)
				.unCentre();
			
			ms.translate(0.075f / 16f, 0, 0);

			for (int segment = 0; segment <= 3; segment++) {
				ms.push();
				float intensity = segment == 3 ? 1.5f : segment + 1;
				float abs = Math.abs(f);
				float flapAngle = MathHelper.sin((float) ((1 - abs) * Math.PI * intensity)) * 30 * f
					* (direction.getAxis() == Axis.X ? 1 : -1);
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

}

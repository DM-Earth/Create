package com.simibubi.create.content.kinetics.fan;

import static net.minecraft.state.property.Properties.FACING;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class EncasedFanRenderer extends KineticBlockEntityRenderer<EncasedFanBlockEntity> {

	public EncasedFanRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(EncasedFanBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {
		if (Backend.canUseInstancing(be.getWorld())) return;

		Direction direction = be.getCachedState()
				.get(FACING);
		VertexConsumer vb = buffer.getBuffer(RenderLayer.getCutoutMipped());

		int lightBehind = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().offset(direction.getOpposite()));
		int lightInFront = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().offset(direction));

		SuperByteBuffer shaftHalf =
				CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getCachedState(), direction.getOpposite());
		SuperByteBuffer fanInner =
				CachedBufferer.partialFacing(AllPartialModels.ENCASED_FAN_INNER, be.getCachedState(), direction.getOpposite());

		float time = AnimationTickHolder.getRenderTime(be.getWorld());
		float speed = be.getSpeed() * 5;
		if (speed > 0)
			speed = MathHelper.clamp(speed, 80, 64 * 20);
		if (speed < 0)
			speed = MathHelper.clamp(speed, -64 * 20, -80);
		float angle = (time * speed * 3 / 10f) % 360;
		angle = angle / 180f * (float) Math.PI;

		standardKineticRotationTransform(shaftHalf, be, lightBehind).renderInto(ms, vb);
		kineticRotationTransform(fanInner, be, direction.getAxis(), angle, lightInFront).renderInto(ms, vb);
	}

}

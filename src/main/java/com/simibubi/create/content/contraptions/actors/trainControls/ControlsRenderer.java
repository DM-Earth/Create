package com.simibubi.create.content.contraptions.actors.trainControls;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class ControlsRenderer {

	public static void render(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
		VertexConsumerProvider buffer, float equipAnimation, float firstLever, float secondLever) {
		BlockState state = context.state;
		Direction facing = state.get(ControlsBlock.FACING);

		SuperByteBuffer cover = CachedBufferer.partial(AllPartialModels.TRAIN_CONTROLS_COVER, state);
		float hAngle = 180 + AngleHelper.horizontalAngle(facing);
		MatrixStack ms = matrices.getModel();
		cover.transform(ms)
			.centre()
			.rotateY(hAngle)
			.unCentre()
			.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld))
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderLayer.getCutoutMipped()));

		double yOffset = MathHelper.lerp(equipAnimation * equipAnimation, -0.15f, 0.05f);

		for (boolean first : Iterate.trueAndFalse) {
			float vAngle = (float) MathHelper.clamp(first ? firstLever * 70 - 25 : secondLever * 15, -45, 45);
			SuperByteBuffer lever = CachedBufferer.partial(AllPartialModels.TRAIN_CONTROLS_LEVER, state);
			ms.push();
			TransformStack.cast(ms)
				.centre()
				.rotateY(hAngle)
				.translate(0, 0, 4 / 16f)
				.rotateX(vAngle - 45)
				.translate(0, yOffset, 0)
				.rotateX(45)
				.unCentre()
				.translate(0, -2 / 16f, -3 / 16f)
				.translate(first ? 0 : 6 / 16f, 0, 0);
			lever.transform(ms)
				.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld))
				.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderLayer.getSolid()));
			ms.pop();
		}

	}

}

package com.simibubi.create.content.contraptions.chassis;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

public class StickerRenderer extends SafeBlockEntityRenderer<StickerBlockEntity> {

	public StickerRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	protected void renderSafe(StickerBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer,
		int light, int overlay) {

		if (Backend.canUseInstancing(be.getWorld())) return;

		BlockState state = be.getCachedState();
		SuperByteBuffer head = CachedBufferer.partial(AllPartialModels.STICKER_HEAD, state);
		float offset = be.piston.getValue(AnimationTickHolder.getPartialTicks(be.getWorld()));

		if (be.getWorld() != MinecraftClient.getInstance().world && !be.isVirtual())
			offset = state.get(StickerBlock.EXTENDED) ? 1 : 0;

		Direction facing = state.get(StickerBlock.FACING);
		head.nudge(be.hashCode())
			.centre()
			.rotateY(AngleHelper.horizontalAngle(facing))
			.rotateX(AngleHelper.verticalAngle(facing) + 90)
			.unCentre()
			.translate(0, (offset * offset) * 4 / 16f, 0);

		head.light(light)
			.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
	}

}

package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class BogeyBlockEntityRenderer<T extends BlockEntity> extends SafeBlockEntityRenderer<T> {

	public BogeyBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

	@Override
	protected void renderSafe(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light,
		int overlay) {
		BlockState blockState = be.getCachedState();
		if (be instanceof AbstractBogeyBlockEntity sbbe) {
			float angle = sbbe.getVirtualAngle(partialTicks);
			if (blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey)
				bogey.render(blockState, angle, ms, partialTicks, buffer, light, overlay, sbbe.getStyle(), sbbe.getBogeyData());
		}
	}

}

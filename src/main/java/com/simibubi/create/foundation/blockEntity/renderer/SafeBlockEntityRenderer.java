package com.simibubi.create.foundation.blockEntity.renderer;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

public abstract class SafeBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	@Override
	public final void render(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light,
		int overlay) {
		if (isInvalid(be))
			return;
		renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
	}

	protected abstract void renderSafe(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light,
		int overlay);

	public boolean isInvalid(T be) {
		return !be.hasWorld() || be.getCachedState()
			.getBlock() == Blocks.AIR;
	}
}

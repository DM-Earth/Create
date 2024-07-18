package com.simibubi.create.foundation.blockEntity.renderer;

import com.simibubi.create.content.redstone.link.LinkRenderer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class SmartBlockEntityRenderer<T extends SmartBlockEntity> extends SafeBlockEntityRenderer<T> {

	public SmartBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
	}
	
	@Override
	protected void renderSafe(T blockEntity, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light,
			int overlay) {
		FilteringRenderer.renderOnBlockEntity(blockEntity, partialTicks, ms, buffer, light, overlay);
		LinkRenderer.renderOnBlockEntity(blockEntity, partialTicks, ms, buffer, light, overlay);
	}

}

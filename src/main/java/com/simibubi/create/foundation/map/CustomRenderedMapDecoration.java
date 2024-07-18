package com.simibubi.create.foundation.map;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapState;

public interface CustomRenderedMapDecoration {
	void render(MatrixStack poseStack, VertexConsumerProvider bufferSource, boolean active, int packedLight, MapState mapData, int index);
}

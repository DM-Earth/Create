package com.simibubi.create.foundation.ponder.element;

import com.simibubi.create.foundation.ponder.PonderWorld;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public abstract class PonderSceneElement extends PonderElement {

	public abstract void renderFirst(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt);
	
	public abstract void renderLayer(PonderWorld world, VertexConsumerProvider buffer, RenderLayer type, MatrixStack ms, float pt);
	
	public abstract void renderLast(PonderWorld world, VertexConsumerProvider buffer, MatrixStack ms, float pt);
	
}

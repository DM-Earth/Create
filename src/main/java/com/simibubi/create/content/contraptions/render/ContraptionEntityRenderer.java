package com.simibubi.create.content.contraptions.render;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ContraptionEntityRenderer<C extends AbstractContraptionEntity> extends EntityRenderer<C> {

	public ContraptionEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(C entity) {
		return null;
	}

	@Override
	public boolean shouldRender(C entity, Frustum clippingHelper, double cameraX, double cameraY,
		double cameraZ) {
		if (entity.getContraption() == null)
			return false;
		if (!entity.isAliveOrStale())
			return false;
		if (!entity.isReadyForRender())
			return false;

		return super.shouldRender(entity, clippingHelper, cameraX, cameraY, cameraZ);
	}

	@Override
	public void render(C entity, float yaw, float partialTicks, MatrixStack ms, VertexConsumerProvider buffers,
		int overlay) {
		super.render(entity, yaw, partialTicks, ms, buffers, overlay);

		Contraption contraption = entity.getContraption();
		if (contraption != null) {
			ContraptionRenderDispatcher.renderFromEntity(entity, contraption, buffers);
		}
	}

}

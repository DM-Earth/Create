package com.simibubi.create.content.equipment.potatoCannon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class PotatoProjectileRenderer extends EntityRenderer<PotatoProjectileEntity> {

	public PotatoProjectileRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(PotatoProjectileEntity entity, float yaw, float pt, MatrixStack ms, VertexConsumerProvider buffer,
		int light) {
		ItemStack item = entity.getItem();
		if (item.isEmpty())
			return;
		ms.push();
		ms.translate(0, entity.getBoundingBox()
			.getYLength() / 2 - 1 / 8f, 0);
		entity.getRenderMode()
			.transform(ms, entity, pt);

		MinecraftClient.getInstance()
			.getItemRenderer()
			.renderItem(item, ModelTransformationMode.GROUND, light, OverlayTexture.DEFAULT_UV, ms, buffer, entity.getWorld(),
				0);
		ms.pop();
	}

	@Override
	public Identifier getTexture(PotatoProjectileEntity entity) {
		return null;
	}

}

package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;

public class BacktankArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {

	public BacktankArmorLayer(FeatureRendererContext<T, M> renderer) {
		super(renderer);
	}

	@Override
	public void render(MatrixStack ms, VertexConsumerProvider buffer, int light, LivingEntity entity, float yaw, float pitch,
		float pt, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
		if (entity.getPose() == EntityPose.SLEEPING)
			return;

		BacktankItem item = BacktankItem.getWornBy(entity);
		if (item == null)
			return;

		M entityModel = getContextModel();
		if (!(entityModel instanceof BipedEntityModel))
			return;

		BipedEntityModel<?> model = (BipedEntityModel<?>) entityModel;
		RenderLayer renderType = TexturedRenderLayers.getEntityCutout();
		BlockState renderedState = item.getBlock().getDefaultState()
				.with(BacktankBlock.HORIZONTAL_FACING, Direction.SOUTH);
		SuperByteBuffer backtank = CachedBufferer.block(renderedState);
		SuperByteBuffer cogs = CachedBufferer.partial(BacktankRenderer.getCogsModel(renderedState), renderedState);

		ms.push();

		model.body.rotate(ms);
		ms.translate(-1 / 2f, 10 / 16f, 1f);
		ms.scale(1, -1, -1);

		backtank.forEntityRender()
			.light(light)
			.renderInto(ms, buffer.getBuffer(renderType));

		cogs.centre()
			.rotateY(180)
			.unCentre()
			.translate(0, 6.5f / 16, 11f / 16)
			.rotate(Direction.EAST, AngleHelper.rad(2 * AnimationTickHolder.getRenderTime(entity.getWorld()) % 360))
			.translate(0, -6.5f / 16, -11f / 16);

		cogs.forEntityRender()
			.light(light)
			.renderInto(ms, buffer.getBuffer(renderType));

		ms.pop();
	}

//	public static void registerOnAll(EntityRenderDispatcher renderManager) {
//		for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values())
//			registerOn(renderer);
//		for (EntityRenderer<?> renderer : renderManager.renderers.values())
//			registerOn(renderer);
//	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void registerOn(EntityRenderer<?> entityRenderer, RegistrationHelper helper) {
		if (!(entityRenderer instanceof LivingEntityRenderer))
			return;
		LivingEntityRenderer<?, ?> livingRenderer = (LivingEntityRenderer<?, ?>) entityRenderer;
		if (!(livingRenderer.getModel() instanceof BipedEntityModel))
			return;
		BacktankArmorLayer<?, ?> layer = new BacktankArmorLayer<>(livingRenderer);
		helper.register(layer);
	}

}

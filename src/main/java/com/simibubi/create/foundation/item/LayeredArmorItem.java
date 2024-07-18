package com.simibubi.create.foundation.item;

import java.util.Map;
import com.simibubi.create.foundation.mixin.accessor.HumanoidArmorLayerAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface LayeredArmorItem extends CustomRenderedArmorItem {
	@Environment(EnvType.CLIENT)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	default void renderArmorPiece(ArmorFeatureRenderer<?, ?, ?> layer, MatrixStack poseStack,
			VertexConsumerProvider bufferSource, LivingEntity entity, EquipmentSlot slot, int light,
			BipedEntityModel<?> originalModel, ItemStack stack) {
		if (!(stack.getItem() instanceof ArmorItem item)) {
			return;
		}
		if (LivingEntity.getPreferredEquipmentSlot(stack) != slot) {
			return;
		}

		HumanoidArmorLayerAccessor accessor = (HumanoidArmorLayerAccessor) layer;
		Map<String, Identifier> locationCache = HumanoidArmorLayerAccessor.create$getArmorTextureCache();
		boolean glint = stack.hasGlint();

		BipedEntityModel<?> innerModel = accessor.create$getInnerModel();
		layer.getContextModel().copyBipedStateTo((BipedEntityModel) innerModel);
		accessor.create$callSetVisible(innerModel, slot);
		String locationStr2 = getArmorTextureLocation(entity, slot, stack, 2);
		Identifier location2 = locationCache.computeIfAbsent(locationStr2, Identifier::new);
		renderModel(poseStack, bufferSource, light, item, innerModel, glint, 1.0F, 1.0F, 1.0F, location2);

		BipedEntityModel<?> outerModel = accessor.create$getOuterModel();
		layer.getContextModel().copyBipedStateTo((BipedEntityModel) outerModel);
		accessor.create$callSetVisible(outerModel, slot);
		String locationStr1 = getArmorTextureLocation(entity, slot, stack, 1);
		Identifier location1 = locationCache.computeIfAbsent(locationStr1, Identifier::new);
		renderModel(poseStack, bufferSource, light, item, outerModel, glint, 1.0F, 1.0F, 1.0F, location1);
	}

	// from HumanoidArmorLayer.renderModel
	private void renderModel(MatrixStack poseStack, VertexConsumerProvider bufferSource, int light, ArmorItem item,
		Model model, boolean glint, float red, float green, float blue, Identifier armorResource) {
		VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderLayer.getArmorCutoutNoCull(armorResource));
		model.render(poseStack, vertexconsumer, light, OverlayTexture.DEFAULT_UV, red, green, blue, 1.0F);
	}

	String getArmorTextureLocation(LivingEntity entity, EquipmentSlot slot, ItemStack stack, int layer);
}

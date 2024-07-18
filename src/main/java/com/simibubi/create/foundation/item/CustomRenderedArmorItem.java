package com.simibubi.create.foundation.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface CustomRenderedArmorItem {
	@Environment(EnvType.CLIENT)
	void renderArmorPiece(ArmorFeatureRenderer<?, ?, ?> layer, MatrixStack poseStack, VertexConsumerProvider bufferSource, LivingEntity entity, EquipmentSlot slot, int light, BipedEntityModel<?> originalModel, ItemStack stack);
}

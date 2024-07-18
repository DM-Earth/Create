package com.simibubi.create.compat.trinkets;

import com.simibubi.create.AllItems;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class GoggleTrinketRenderer implements TrinketRenderer {
	@Override
	public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> model,
					   MatrixStack matrices, VertexConsumerProvider multiBufferSource, int light, LivingEntity entity,
					   float limbAngle, float limbDistance, float tickDelta, float animationProgress,
					   float headYaw, float headPitch) {
		if (AllItems.GOGGLES.isIn(stack) &&
				model instanceof PlayerEntityModel playerModel &&
				entity instanceof AbstractClientPlayerEntity player) {

			// Translate and rotate with our head
			matrices.push();
			TrinketRenderer.followBodyRotations(entity, playerModel);
			TrinketRenderer.translateToFace(matrices, playerModel, player, headYaw, headPitch);

			// Translate and scale to our head
			matrices.translate(0, 0, 0.3);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
			matrices.scale(0.625f, 0.625f, 0.625f);

			if (headOccupied(entity)) {
				matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
				matrices.translate(0, -0.25, 0);
			}

			// Render
			MinecraftClient mc = MinecraftClient.getInstance();
			mc.getItemRenderer()
					.renderItem(stack, ModelTransformationMode.HEAD, light, OverlayTexture.DEFAULT_UV, matrices,
							multiBufferSource, mc.world, 0);
			matrices.pop();
		}
	}

	public static boolean headOccupied(LivingEntity entity) {
		if (!entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty())
			return true;
		return TrinketsApi.getTrinketComponent(entity)
				.filter(component -> {							 // guaranteed  // may be null
					TrinketInventory inv = component.getInventory().get("head").get("hat");
					return inv != null && !inv.isEmpty();
				}).isPresent();
	}
}

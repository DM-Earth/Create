package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public class NetheriteBacktankFirstPersonRenderer {

	private static final Identifier BACKTANK_ARMOR_LOCATION =
		Create.asResource("textures/models/armor/netherite_diving_arm.png");

	private static boolean rendererActive = false;

	public static void clientTick() {
		MinecraftClient mc = MinecraftClient.getInstance();
		rendererActive =
			mc.player != null && AllItems.NETHERITE_BACKTANK.isIn(mc.player.getEquippedStack(EquipmentSlot.CHEST));
	}

	public static boolean onRenderPlayerHand(MatrixStack poseStack, VertexConsumerProvider buffer, int packedLight, AbstractClientPlayerEntity player, Arm arm) {
		if (!rendererActive)
			return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (!(mc.getEntityRenderDispatcher()
			.getRenderer(player) instanceof PlayerEntityRenderer pr))
			return false;

		PlayerEntityModel<AbstractClientPlayerEntity> model = pr.getModel();
		model.handSwingProgress = 0.0F;
		model.sneaking = false;
		model.leaningPitch = 0.0F;
		model.setAngles(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		ModelPart armPart = arm == Arm.LEFT ? model.leftSleeve : model.rightSleeve;
		armPart.pitch = 0.0F;
		armPart.render(poseStack, buffer.getBuffer(RenderLayer.getEntitySolid(BACKTANK_ARMOR_LOCATION)),
			LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
		return true;
	}

}

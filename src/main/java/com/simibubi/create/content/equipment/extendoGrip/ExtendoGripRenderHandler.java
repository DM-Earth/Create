package com.simibubi.create.content.equipment.extendoGrip;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import io.github.fabricators_of_create.porting_lib.event.client.RenderHandCallback.RenderHandEvent;
import io.github.fabricators_of_create.porting_lib.util.FirstPersonRendererHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class ExtendoGripRenderHandler {

	public static float mainHandAnimation;
	public static float lastMainHandAnimation;
	public static PartialModel pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;

	public static void tick() {
		lastMainHandAnimation = mainHandAnimation;
		mainHandAnimation *= MathHelper.clamp(mainHandAnimation, 0.8f, 0.99f);

		pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;
		if (!AllItems.EXTENDO_GRIP.isIn(getRenderedOffHandStack()))
			return;
		ItemStack main = getRenderedMainHandStack();
		if (main.isEmpty())
			return;
		if (!(main.getItem() instanceof BlockItem))
			return;
		if (!MinecraftClient.getInstance()
			.getItemRenderer()
			.getModel(main, null, null, 0)
			.hasDepth())
			return;
		pose = AllPartialModels.DEPLOYER_HAND_HOLDING;
	}

	public static void onRenderPlayerHand(RenderHandEvent event) {
		ItemStack heldItem = event.getItemStack();
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		boolean rightHand = event.getHand() == Hand.MAIN_HAND ^ player.getMainArm() == Arm.LEFT;

		ItemStack offhandItem = getRenderedOffHandStack();
		boolean notInOffhand = !AllItems.EXTENDO_GRIP.isIn(offhandItem);
		if (notInOffhand && !AllItems.EXTENDO_GRIP.isIn(heldItem))
			return;

		MatrixStack ms = event.getPoseStack();
		TransformStack msr = TransformStack.cast(ms);
		AbstractClientPlayerEntity abstractclientplayerentity = mc.player;
		RenderSystem.setShaderTexture(0, abstractclientplayerentity.getSkinTexture());

		float flip = rightHand ? 1.0F : -1.0F;
		float swingProgress = event.getSwingProgress();
		boolean blockItem = heldItem.getItem() instanceof BlockItem;
		float equipProgress = blockItem ? 0 : event.getEquipProgress() / 4;

		ms.push();
		if (event.getHand() == Hand.MAIN_HAND) {

			if (1 - swingProgress > mainHandAnimation && swingProgress > 0)
				mainHandAnimation = 0.95f;
			float animation = MathHelper.lerp(AnimationTickHolder.getPartialTicks(),
											  ExtendoGripRenderHandler.lastMainHandAnimation,
											  ExtendoGripRenderHandler.mainHandAnimation);
			animation = animation * animation * animation;

			ms.translate(flip * (0.64000005F - .1f), -0.4F + equipProgress * -0.6F, -0.71999997F + .3f);

			ms.push();
			msr.rotateY(flip * 75.0F);
			ms.translate(flip * -1.0F, 3.6F, 3.5F);
			msr.rotateZ(flip * 120)
				.rotateX(200)
				.rotateY(flip * -135.0F);
			ms.translate(flip * 5.6F, 0.0F, 0.0F);
			msr.rotateY(flip * 40.0F);
			ms.translate(flip * 0.05f, -0.3f, -0.3f);

			PlayerEntityRenderer playerrenderer = (PlayerEntityRenderer) mc.getEntityRenderDispatcher()
				.getRenderer(player);
			if (rightHand)
				playerrenderer.renderRightArm(event.getPoseStack(), event.getMultiBufferSource(),
					event.getPackedLight(), player);
			else
				playerrenderer.renderLeftArm(event.getPoseStack(), event.getMultiBufferSource(),
					event.getPackedLight(), player);
			ms.pop();

			// Render gun
			ms.push();
			ms.translate(flip * -0.1f, 0, -0.3f);
			HeldItemRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getHeldItemRenderer();
			ModelTransformationMode transform =
				rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND;
			firstPersonRenderer.renderItem(mc.player, notInOffhand ? heldItem : offhandItem, transform, !rightHand,
				event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());

			if (!notInOffhand) {
				mc.getItemRenderer().getModel(offhandItem, null, null, 0).getTransformation().getTransformation(transform).apply(!rightHand, ms);
				ms.translate(flip * -.05f, .15f, -1.2f);
				ms.translate(0, 0, -animation * 2.25f);
				if (blockItem && mc.getItemRenderer()
					.getModel(heldItem, null, null, 0)
					.hasDepth()) {
					msr.rotateY(flip * 45);
					ms.translate(flip * 0.15f, -0.15f, -.05f);
					ms.scale(1.25f, 1.25f, 1.25f);
				}

				firstPersonRenderer.renderItem(mc.player, heldItem, transform, !rightHand, event.getPoseStack(),
					event.getMultiBufferSource(), event.getPackedLight());
			}

			ms.pop();
		}
		ms.pop();
		event.setCanceled(true);
	}

	private static ItemStack getRenderedMainHandStack() {
		return FirstPersonRendererHelper.getStackInMainHand(MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer());
	}

	private static ItemStack getRenderedOffHandStack() {
		return FirstPersonRendererHelper.getStackInOffHand(MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer());
	}

}

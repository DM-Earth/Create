package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fabricators_of_create.porting_lib.event.client.RenderHandCallback;
import io.github.fabricators_of_create.porting_lib.event.client.RenderHandCallback.RenderHandEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public abstract class ShootableGadgetRenderHandler {

	protected float leftHandAnimation;
	protected float rightHandAnimation;
	protected float lastLeftHandAnimation;
	protected float lastRightHandAnimation;
	protected boolean dontReequipLeft;
	protected boolean dontReequipRight;

	public void tick() {
		lastLeftHandAnimation = leftHandAnimation;
		lastRightHandAnimation = rightHandAnimation;
		leftHandAnimation *= animationDecay();
		rightHandAnimation *= animationDecay();
	}

	public float getAnimation(boolean rightHand, float partialTicks) {
		return MathHelper.lerp(partialTicks, rightHand ? lastRightHandAnimation : lastLeftHandAnimation,
			rightHand ? rightHandAnimation : leftHandAnimation);
	}

	protected float animationDecay() {
		return 0.8f;
	}

	public void shoot(Hand hand, Vec3d location) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		boolean rightHand = hand == Hand.MAIN_HAND ^ player.getMainArm() == Arm.LEFT;
		if (rightHand) {
			rightHandAnimation = .2f;
			dontReequipRight = false;
		} else {
			leftHandAnimation = .2f;
			dontReequipLeft = false;
		}
		playSound(hand, location);
	}

	protected abstract void playSound(Hand hand, Vec3d position);

	protected abstract boolean appliesTo(ItemStack stack);

	protected abstract void transformTool(MatrixStack ms, float flip, float equipProgress, float recoil, float pt);

	protected abstract void transformHand(MatrixStack ms, float flip, float equipProgress, float recoil, float pt);

	public void registerListeners() {
		RenderHandCallback.EVENT.register(this::onRenderPlayerHand);
	}

	protected void onRenderPlayerHand(RenderHandEvent event) {
		ItemStack heldItem = event.getItemStack();
		if (!appliesTo(heldItem))
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		AbstractClientPlayerEntity player = mc.player;
		PlayerEntityRenderer playerrenderer = (PlayerEntityRenderer) mc.getEntityRenderDispatcher()
			.getRenderer(player);
		HeldItemRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getHeldItemRenderer();

		MatrixStack ms = event.getPoseStack();
		VertexConsumerProvider buffer = event.getMultiBufferSource();
		int light = event.getPackedLight();
		float pt = event.getPartialTicks();

		boolean rightHand = event.getHand() == Hand.MAIN_HAND ^ mc.player.getMainArm() == Arm.LEFT;
		float recoil = rightHand ? MathHelper.lerp(pt, lastRightHandAnimation, rightHandAnimation)
			: MathHelper.lerp(pt, lastLeftHandAnimation, leftHandAnimation);
		float equipProgress = event.getEquipProgress();

		if (rightHand && (rightHandAnimation > .01f || dontReequipRight))
			equipProgress = 0;
		if (!rightHand && (leftHandAnimation > .01f || dontReequipLeft))
			equipProgress = 0;

		// Render arm
		ms.push();
		RenderSystem.setShaderTexture(0, player.getSkinTexture());

		float flip = rightHand ? 1.0F : -1.0F;
		float f1 = MathHelper.sqrt(event.getSwingProgress());
		float f2 = -0.3F * MathHelper.sin(f1 * (float) Math.PI);
		float f3 = 0.4F * MathHelper.sin(f1 * ((float) Math.PI * 2F));
		float f4 = -0.4F * MathHelper.sin(event.getSwingProgress() * (float) Math.PI);
		float f5 = MathHelper.sin(event.getSwingProgress() * event.getSwingProgress() * (float) Math.PI);
		float f6 = MathHelper.sin(f1 * (float) Math.PI);

		ms.translate(flip * (f2 + 0.64F - .1f), f3 + -0.4F + equipProgress * -0.6F, f4 + -0.72F + .3f + recoil);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flip * 75.0F));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flip * f6 * 70.0F));
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flip * f5 * -20.0F));
		ms.translate(flip * -1.0F, 3.6F, 3.5F);
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flip * 120.0F));
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flip * -135.0F));
		ms.translate(flip * 5.6F, 0.0F, 0.0F);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flip * 40.0F));
		transformHand(ms, flip, equipProgress, recoil, pt);
		if (rightHand)
			playerrenderer.renderRightArm(ms, buffer, light, player);
		else
			playerrenderer.renderLeftArm(ms, buffer, light, player);
		ms.pop();

		// Render gadget
		ms.push();
		ms.translate(flip * (f2 + 0.64F - .1f), f3 + -0.4F + equipProgress * -0.6F, f4 + -0.72F - 0.1f + recoil);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(flip * f6 * 70.0F));
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flip * f5 * -20.0F));
		transformTool(ms, flip, equipProgress, recoil, pt);
		firstPersonRenderer.renderItem(mc.player, heldItem,
			rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
				: ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
			!rightHand, ms, buffer, light);
		ms.pop();

		event.setCanceled(true);
	}

	public void dontAnimateItem(Hand hand) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		boolean rightHand = hand == Hand.MAIN_HAND ^ player.getMainArm() == Arm.LEFT;
		dontReequipRight |= rightHand;
		dontReequipLeft |= !rightHand;
	}

}

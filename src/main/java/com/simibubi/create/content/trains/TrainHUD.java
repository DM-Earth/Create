package com.simibubi.create.content.trains;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.structure.StructureTemplate.StructureBlockInfo;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

public class TrainHUD {

	static LerpedFloat displayedSpeed = LerpedFloat.linear();
	static LerpedFloat displayedThrottle = LerpedFloat.linear();
	static LerpedFloat displayedPromptSize = LerpedFloat.linear();

	static Double editedThrottle = null;
	static int hudPacketCooldown = 5;
	static int honkPacketCooldown = 5;

	public static Text currentPrompt;
	public static boolean currentPromptShadow;
	public static int promptKeepAlive = 0;

	static boolean usedToHonk;

	public static void tick() {
		if (promptKeepAlive > 0)
			promptKeepAlive--;
		else
			currentPrompt = null;

		MinecraftClient mc = MinecraftClient.getInstance();
		displayedPromptSize.chase(currentPrompt != null ? mc.textRenderer.getWidth(currentPrompt) + 17 : 0, .5f, Chaser.EXP);
		displayedPromptSize.tickChaser();

		Carriage carriage = getCarriage();
		if (carriage == null)
			return;

		Train train = carriage.train;
		double value =
			Math.abs(train.speed) / (train.maxSpeed() * AllConfigs.server().trains.manualTrainSpeedModifier.getF());
		value = MathHelper.clamp(value + 0.05f, 0, 1);

		displayedSpeed.chase((int) (value * 18) / 18f, .5f, Chaser.EXP);
		displayedSpeed.tickChaser();
		displayedThrottle.chase(editedThrottle != null ? editedThrottle : train.throttle, .75f, Chaser.EXP);
		displayedThrottle.tickChaser();

		boolean isSprintKeyPressed = ControlsUtil.isActuallyPressed(mc.options.sprintKey);

		if (isSprintKeyPressed && honkPacketCooldown-- <= 0) {
			train.determineHonk(mc.world);
			if (train.lowHonk != null) {
				AllPackets.getChannel().sendToServer(new HonkPacket.Serverbound(train, true));
				honkPacketCooldown = 5;
				usedToHonk = true;
			}
		}

		if (!isSprintKeyPressed && usedToHonk) {
			AllPackets.getChannel().sendToServer(new HonkPacket.Serverbound(train, false));
			honkPacketCooldown = 0;
			usedToHonk = false;
		}

		if (editedThrottle == null)
			return;
		if (MathHelper.approximatelyEquals(editedThrottle, train.throttle)) {
			editedThrottle = null;
			hudPacketCooldown = 5;
			return;
		}

		if (hudPacketCooldown-- <= 0) {
			AllPackets.getChannel().sendToServer(new TrainHUDUpdatePacket.Serverbound(train, editedThrottle));
			hudPacketCooldown = 5;
		}
	}

	private static Carriage getCarriage() {
		if (!(ControlsHandler.getContraption() instanceof CarriageContraptionEntity cce))
			return null;
		return cce.getCarriage();
	}

	public static void renderOverlay(DrawContext graphics, float partialTicks, Window window) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR)
			return;
		if (!(ControlsHandler.getContraption() instanceof CarriageContraptionEntity cce))
			return;
		Carriage carriage = cce.getCarriage();
		if (carriage == null)
			return;
		Entity cameraEntity = MinecraftClient.getInstance()
			.getCameraEntity();
		if (cameraEntity == null)
			return;
		BlockPos localPos = ControlsHandler.getControlsPos();
		if (localPos == null)
			return;

		MatrixStack poseStack = graphics.getMatrices();
		poseStack.push();
		poseStack.translate(window.getScaledWidth() / 2 - 91, window.getScaledHeight() - 29, 0);

		// Speed, Throttle

		AllGuiTextures.TRAIN_HUD_FRAME.render(graphics, -2, 1);
		AllGuiTextures.TRAIN_HUD_SPEED_BG.render(graphics, 0, 0);

		int w = (int) (AllGuiTextures.TRAIN_HUD_SPEED.width * displayedSpeed.getValue(partialTicks));
		int h = AllGuiTextures.TRAIN_HUD_SPEED.height;

		graphics.drawTexture(AllGuiTextures.TRAIN_HUD_SPEED.location, 0, 0, 0, AllGuiTextures.TRAIN_HUD_SPEED.startX,
			AllGuiTextures.TRAIN_HUD_SPEED.startY, w, h, 256, 256);

		int promptSize = (int) displayedPromptSize.getValue(partialTicks);
		if (promptSize > 1) {

			poseStack.push();
			poseStack.translate(promptSize / -2f + 91, -27, 100);

			AllGuiTextures.TRAIN_PROMPT_L.render(graphics, -3, 0);
			AllGuiTextures.TRAIN_PROMPT_R.render(graphics, promptSize, 0);
			graphics.drawTexture(AllGuiTextures.TRAIN_PROMPT.location, 0, 0, 0, AllGuiTextures.TRAIN_PROMPT.startX + (128 - promptSize / 2f),
				AllGuiTextures.TRAIN_PROMPT.startY, promptSize, AllGuiTextures.TRAIN_PROMPT.height, 256, 256);

			poseStack.pop();

			TextRenderer font = mc.textRenderer;
			if (currentPrompt != null && font.getWidth(currentPrompt) < promptSize - 10) {
				poseStack.push();
				poseStack.translate(font.getWidth(currentPrompt) / -2f + 82, -27, 100);
				if (currentPromptShadow)
					graphics.drawTextWithShadow(font, currentPrompt, 9, 4, 0x544D45);
				else
					graphics.drawText(font, currentPrompt, 9, 4, 0x544D45, false);
				poseStack.pop();
			}
		}

		AllGuiTextures.TRAIN_HUD_DIRECTION.render(graphics, 77, -20);

		w = (int) (AllGuiTextures.TRAIN_HUD_THROTTLE.width * (1 - displayedThrottle.getValue(partialTicks)));
		int invW = AllGuiTextures.TRAIN_HUD_THROTTLE.width - w;
		graphics.drawTexture(AllGuiTextures.TRAIN_HUD_THROTTLE.location, invW, 0, 0, AllGuiTextures.TRAIN_HUD_THROTTLE.startX + invW,
			AllGuiTextures.TRAIN_HUD_THROTTLE.startY, w, h, 256, 256);
		AllGuiTextures.TRAIN_HUD_THROTTLE_POINTER.render(graphics,
			Math.max(1, AllGuiTextures.TRAIN_HUD_THROTTLE.width - w) - 3, -2);

		// Direction

		StructureBlockInfo info = cce.getContraption()
			.getBlocks()
			.get(localPos);
		Direction initialOrientation = cce.getInitialOrientation()
			.rotateYCounterclockwise();
		boolean inverted = false;
		if (info != null && info.state().contains(ControlsBlock.FACING))
			inverted = !info.state().get(ControlsBlock.FACING)
				.equals(initialOrientation);

		boolean reversing = ControlsHandler.currentlyPressed.contains(1);
		inverted ^= reversing;
		int angleOffset = (ControlsHandler.currentlyPressed.contains(2) ? -45 : 0)
			+ (ControlsHandler.currentlyPressed.contains(3) ? 45 : 0);
		if (reversing)
			angleOffset *= -1;

		float snapSize = 22.5f;
		float diff = AngleHelper.getShortestAngleDiff(cameraEntity.getYaw(), cce.yaw) + (inverted ? -90 : 90);
		if (Math.abs(diff) < 60)
			diff = 0;

		float angle = diff + angleOffset;
		float snappedAngle = (snapSize * Math.round(angle / snapSize)) % 360f;

		poseStack.translate(91, -9, 0);
		poseStack.scale(0.925f, 0.925f, 1);
		PlacementHelpers.textured(poseStack, 0, 0, 1, snappedAngle);

		poseStack.pop();
	}

	public static boolean onScroll(double delta) {
		Carriage carriage = getCarriage();
		if (carriage == null)
			return false;

		double prevThrottle = editedThrottle == null ? carriage.train.throttle : editedThrottle;
		editedThrottle = MathHelper.clamp(prevThrottle + (delta > 0 ? 1 : -1) / 18f, 1 / 18f, 1);
		return true;
	}

}

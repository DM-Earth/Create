package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.utility.Color;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ValueSettingsClient {

	private MinecraftClient mc;

	public int interactHeldTicks = -1;
	public BlockPos interactHeldPos = null;
	public BehaviourType<?> interactHeldBehaviour = null;
	public Hand interactHeldHand = null;
	public Direction interactHeldFace = null;

	public List<MutableText> lastHoverTip;
	public int hoverTicks;
	public int hoverWarmup;

	public ValueSettingsClient() {
		mc = MinecraftClient.getInstance();
	}

	public void cancelIfWarmupAlreadyStarted(BlockPos interactPos, MutableBoolean cancelled) {
		if (interactHeldTicks != -1 && interactPos.equals(interactHeldPos))
			cancelled.setTrue();
	}

	public void startInteractionWith(BlockPos pos, BehaviourType<?> behaviourType, Hand hand,
		Direction side) {
		interactHeldTicks = 0;
		interactHeldPos = pos;
		interactHeldBehaviour = behaviourType;
		interactHeldHand = hand;
		interactHeldFace = side;
	}

	public void cancelInteraction() {
		interactHeldTicks = -1;
	}

	public void tick() {
		if (hoverWarmup > 0)
			hoverWarmup--;
		if (hoverTicks > 0)
			hoverTicks--;
		if (interactHeldTicks == -1)
			return;
		PlayerEntity player = mc.player;

		if (!ValueSettingsInputHandler.canInteract(player) || AllBlocks.CLIPBOARD.isIn(player.getMainHandStack())) {
			cancelInteraction();
			return;
		}
		HitResult hitResult = mc.crosshairTarget;
		if (!(hitResult instanceof BlockHitResult blockHitResult) || !blockHitResult.getBlockPos()
			.equals(interactHeldPos)) {
			cancelInteraction();
			return;
		}
		BlockEntityBehaviour behaviour = BlockEntityBehaviour.get(mc.world, interactHeldPos, interactHeldBehaviour);
		if (!(behaviour instanceof ValueSettingsBehaviour valueSettingBehaviour)
			|| !valueSettingBehaviour.testHit(blockHitResult.getPos())) {
			cancelInteraction();
			return;
		}
		if (!mc.options.useKey.isPressed()) {
			AllPackets.getChannel()
				.sendToServer(
					new ValueSettingsPacket(interactHeldPos, 0, 0, interactHeldHand, interactHeldFace, false));
			cancelInteraction();
			return;
		}

		if (interactHeldTicks > 3)
			player.handSwinging = false;
		if (interactHeldTicks++ < 5)
			return;
		ScreenOpener
			.open(new ValueSettingsScreen(interactHeldPos, valueSettingBehaviour.createBoard(player, blockHitResult),
				valueSettingBehaviour.getValueSettings(), valueSettingBehaviour::newSettingHovered));
		interactHeldTicks = -1;
	}

	public void showHoverTip(List<MutableText> tip) {
		if (mc.currentScreen != null)
			return;
		if (hoverWarmup < 6) {
			hoverWarmup += 2;
			return;
		} else
			hoverWarmup++;
		hoverTicks = hoverTicks == 0 ? 11 : Math.max(hoverTicks, 6);
		lastHoverTip = tip;
	}

	public void render(DrawContext graphics, int width, int height) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || !ValueSettingsInputHandler.canInteract(mc.player))
			return;
		if (hoverTicks == 0 || lastHoverTip == null)
			return;

		int x = width / 2;
		int y = height - 75 - lastHoverTip.size() * 12;
		float alpha = hoverTicks > 5 ? (11 - hoverTicks) / 5f : Math.min(1, hoverTicks / 5f);

		Color color = new Color(0xffffff);
		Color titleColor = new Color(0xFBDC7D);
		color.setAlpha(alpha);
		titleColor.setAlpha(alpha);

		for (int i = 0; i < lastHoverTip.size(); i++) {
			MutableText mutableComponent = lastHoverTip.get(i);
			graphics.drawTextWithShadow(mc.textRenderer, mutableComponent, x - mc.textRenderer.getWidth(mutableComponent) / 2, y,
				(i == 0 ? titleColor : color).getRGB());
			y += 12;
		}
	}

}

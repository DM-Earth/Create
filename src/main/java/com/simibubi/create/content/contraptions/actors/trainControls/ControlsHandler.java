package com.simibubi.create.content.contraptions.actors.trainControls;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.Lang;

public class ControlsHandler {

	public static Collection<Integer> currentlyPressed = new HashSet<>();

	public static int PACKET_RATE = 5;
	private static int packetCooldown;

	private static WeakReference<AbstractContraptionEntity> entityRef = new WeakReference<>(null);
	private static BlockPos controlsPos;

	public static void levelUnloaded(WorldAccess level) {
		packetCooldown = 0;
		entityRef = new WeakReference<>(null);
		controlsPos = null;
		currentlyPressed.clear();
	}

	public static void startControlling(AbstractContraptionEntity entity, BlockPos controllerLocalPos) {
		entityRef = new WeakReference<AbstractContraptionEntity>(entity);
		controlsPos = controllerLocalPos;

		MinecraftClient.getInstance().player.sendMessage(
			Lang.translateDirect("contraption.controls.start_controlling", entity.getContraptionName()), true);
	}

	public static void stopControlling() {
		ControlsUtil.getControls()
			.forEach(kb -> kb.setPressed(ControlsUtil.isActuallyPressed(kb)));
		AbstractContraptionEntity abstractContraptionEntity = entityRef.get();

		if (!currentlyPressed.isEmpty() && abstractContraptionEntity != null)
			AllPackets.getChannel().sendToServer(new ControlsInputPacket(currentlyPressed, false,
				abstractContraptionEntity.getId(), controlsPos, false));

		packetCooldown = 0;
		entityRef = new WeakReference<>(null);
		controlsPos = null;
		currentlyPressed.clear();

		MinecraftClient.getInstance().player.sendMessage(Lang.translateDirect("contraption.controls.stop_controlling"),
			true);
	}

	public static void tick() {
		AbstractContraptionEntity entity = entityRef.get();
		if (entity == null)
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		if (entity.isRemoved() || InputUtil.isKeyPressed(MinecraftClient.getInstance()
			.getWindow()
			.getHandle(), GLFW.GLFW_KEY_ESCAPE)) {
			BlockPos pos = controlsPos;
			stopControlling();
			AllPackets.getChannel()
				.sendToServer(new ControlsInputPacket(currentlyPressed, false, entity.getId(), pos, true));
			return;
		}

		Vector<KeyBinding> controls = ControlsUtil.getControls();
		Collection<Integer> pressedKeys = new HashSet<>();
		for (int i = 0; i < controls.size(); i++) {
			if (ControlsUtil.isActuallyPressed(controls.get(i)))
				pressedKeys.add(i);
		}

		Collection<Integer> newKeys = new HashSet<>(pressedKeys);
		Collection<Integer> releasedKeys = currentlyPressed;
		newKeys.removeAll(releasedKeys);
		releasedKeys.removeAll(pressedKeys);

		// Released Keys
		if (!releasedKeys.isEmpty()) {
			AllPackets.getChannel()
				.sendToServer(new ControlsInputPacket(releasedKeys, false, entity.getId(), controlsPos, false));
//			AllSoundEvents.CONTROLLER_CLICK.playAt(player.level, player.blockPosition(), 1f, .5f, true);
		}

		// Newly Pressed Keys
		if (!newKeys.isEmpty()) {
			AllPackets.getChannel().sendToServer(new ControlsInputPacket(newKeys, true, entity.getId(), controlsPos, false));
			packetCooldown = PACKET_RATE;
//			AllSoundEvents.CONTROLLER_CLICK.playAt(player.level, player.blockPosition(), 1f, .75f, true);
		}

		// Keepalive Pressed Keys
		if (packetCooldown == 0) {
//			if (!pressedKeys.isEmpty()) {
				AllPackets.getChannel()
					.sendToServer(new ControlsInputPacket(pressedKeys, true, entity.getId(), controlsPos, false));
				packetCooldown = PACKET_RATE;
//			}
		}

		currentlyPressed = pressedKeys;
		controls.forEach(kb -> kb.setPressed(false));
	}

	@Nullable
	public static AbstractContraptionEntity getContraption() {
		return entityRef.get();
	}

	@Nullable
	public static BlockPos getControlsPos() {
		return controlsPos;
	}

}

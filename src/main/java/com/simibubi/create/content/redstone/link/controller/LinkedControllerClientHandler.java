package com.simibubi.create.content.redstone.link.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.ControlsUtil;
import com.simibubi.create.foundation.utility.Lang;
import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper;

public class LinkedControllerClientHandler {

	public static Mode MODE = Mode.IDLE;
	public static int PACKET_RATE = 5;
	public static Collection<Integer> currentlyPressed = new HashSet<>();
	private static BlockPos lecternPos;
	private static BlockPos selectedLocation = BlockPos.ORIGIN;
	private static int packetCooldown;

	public static void toggleBindMode(BlockPos location) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.BIND;
			selectedLocation = location;
		} else {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static void toggle() {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = null;
		} else {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static void activateInLectern(BlockPos lecternAt) {
		if (MODE == Mode.IDLE) {
			MODE = Mode.ACTIVE;
			lecternPos = lecternAt;
		}
	}

	public static void deactivateInLectern() {
		if (MODE == Mode.ACTIVE && inLectern()) {
			MODE = Mode.IDLE;
			onReset();
		}
	}

	public static boolean inLectern() {
		return lecternPos != null;
	}

	protected static void onReset() {
		ControlsUtil.getControls()
			.forEach(kb -> kb.setPressed(ControlsUtil.isActuallyPressed(kb)));
		packetCooldown = 0;
		selectedLocation = BlockPos.ORIGIN;

		if (inLectern())
			AllPackets.getChannel().sendToServer(new LinkedControllerStopLecternPacket(lecternPos));
		lecternPos = null;

		if (!currentlyPressed.isEmpty())
			AllPackets.getChannel().sendToServer(new LinkedControllerInputPacket(currentlyPressed, false));
		currentlyPressed.clear();

		LinkedControllerItemRenderer.resetButtons();
	}

	public static void tick() {
		LinkedControllerItemRenderer.tick();

		if (MODE == Mode.IDLE)
			return;
		if (packetCooldown > 0)
			packetCooldown--;

		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		ItemStack heldItem = player.getMainHandStack();

		if (player.isSpectator()) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		if (!inLectern() && !AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
			heldItem = player.getOffHandStack();
			if (!AllItems.LINKED_CONTROLLER.isIn(heldItem)) {
				MODE = Mode.IDLE;
				onReset();
				return;
			}
		}

		if (inLectern() && AllBlocks.LECTERN_CONTROLLER.get()
			.getBlockEntityOptional(mc.world, lecternPos)
			.map(be -> !be.isUsedBy(mc.player))
			.orElse(true)) {
			deactivateInLectern();
			return;
		}

		if (mc.currentScreen != null) {
			MODE = Mode.IDLE;
			onReset();
			return;
		}

		if (InputUtil.isKeyPressed(mc.getWindow()
			.getHandle(), GLFW.GLFW_KEY_ESCAPE)) {
			MODE = Mode.IDLE;
			onReset();
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

		if (MODE == Mode.ACTIVE) {
			// Released Keys
			if (!releasedKeys.isEmpty()) {
				AllPackets.getChannel().sendToServer(new LinkedControllerInputPacket(releasedKeys, false, lecternPos));
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.getWorld(), player.getBlockPos(), 1f, .5f, true);
			}

			// Newly Pressed Keys
			if (!newKeys.isEmpty()) {
				AllPackets.getChannel().sendToServer(new LinkedControllerInputPacket(newKeys, true, lecternPos));
				packetCooldown = PACKET_RATE;
				AllSoundEvents.CONTROLLER_CLICK.playAt(player.getWorld(), player.getBlockPos(), 1f, .75f, true);
			}

			// Keepalive Pressed Keys
			if (packetCooldown == 0) {
				if (!pressedKeys.isEmpty()) {
					AllPackets.getChannel().sendToServer(new LinkedControllerInputPacket(pressedKeys, true, lecternPos));
					packetCooldown = PACKET_RATE;
				}
			}
		}

		if (MODE == Mode.BIND) {
			VoxelShape shape = mc.world.getBlockState(selectedLocation)
				.getOutlineShape(mc.world, selectedLocation);
			if (!shape.isEmpty())
				CreateClient.OUTLINER.showAABB("controller", shape.getBoundingBox()
					.offset(selectedLocation))
					.colored(0xB73C2D)
					.lineWidth(1 / 16f);

			for (Integer integer : newKeys) {
				LinkBehaviour linkBehaviour = BlockEntityBehaviour.get(mc.world, selectedLocation, LinkBehaviour.TYPE);
				if (linkBehaviour != null) {
					AllPackets.getChannel().sendToServer(new LinkedControllerBindPacket(integer, selectedLocation));
					Lang.translate("linked_controller.key_bound", controls.get(integer)
						.getBoundKeyLocalizedText()
						.getString())
						.sendStatus(mc.player);
				}
				MODE = Mode.IDLE;
				break;
			}
		}

		currentlyPressed = pressedKeys;
		controls.forEach(kb -> kb.setPressed(false));
	}

	public static void renderOverlay(DrawContext graphics, float partialTicks, Window window) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden)
			return;
		if (MODE != Mode.BIND)
			return;

		MatrixStack poseStack = graphics.getMatrices();
		poseStack.push();
		Screen tooltipScreen = new Screen(Components.immutableEmpty()) {
		};
		tooltipScreen.init(mc, window.getScaledWidth(), window.getScaledHeight());

		Object[] keys = new Object[6];
		Vector<KeyBinding> controls = ControlsUtil.getControls();
		for (int i = 0; i < controls.size(); i++) {
			KeyBinding keyBinding = controls.get(i);
			keys[i] = keyBinding.getBoundKeyLocalizedText()
				.getString();
		}

		List<Text> list = new ArrayList<>();
		list.add(Lang.translateDirect("linked_controller.bind_mode")
			.formatted(Formatting.GOLD));
		list.addAll(TooltipHelper.cutTextComponent(Lang.translateDirect("linked_controller.press_keybind", keys),
			Palette.ALL_GRAY));

		int width = 0;
		int height = list.size() * mc.textRenderer.fontHeight;
		for (Text iTextComponent : list)
			width = Math.max(width, mc.textRenderer.getWidth(iTextComponent));
		int x = (mc.getWindow().getScaledWidth() / 3) - width / 2;
		int y = mc.getWindow().getScaledHeight() - height - 24;

		// TODO
		graphics.drawTooltip(MinecraftClient.getInstance().textRenderer, list, x, y);

		poseStack.pop();
	}

	public enum Mode {
		IDLE, ACTIVE, BIND
	}

}

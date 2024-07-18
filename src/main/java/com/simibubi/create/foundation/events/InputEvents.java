package com.simibubi.create.foundation.events;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.elevator.ElevatorControlsHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.trains.TrainHUD;
import com.simibubi.create.content.trains.entity.TrainRelocator;
import com.simibubi.create.content.trains.track.CurvedTrackInteraction;

import io.github.fabricators_of_create.porting_lib.event.client.InteractEvents;
import io.github.fabricators_of_create.porting_lib.event.client.KeyInputCallback;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents;
import io.github.fabricators_of_create.porting_lib.event.client.MouseInputEvents.Action;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

public class InputEvents {

	public static void onKeyInput(int key, int scancode, int action, int mods) {
		if (MinecraftClient.getInstance().currentScreen != null)
			return;

		boolean pressed = !(action == 0);

		CreateClient.SCHEMATIC_HANDLER.onKeyInput(key, pressed);
		ToolboxHandlerClient.onKeyInput(key, pressed);
	}

	public static boolean onMouseScrolled(double deltaX, double delta /* Y */) {
		if (MinecraftClient.getInstance().currentScreen != null)
			return false;

//		CollisionDebugger.onScroll(delta);
		boolean cancelled = CreateClient.SCHEMATIC_HANDLER.mouseScrolled(delta)
			|| CreateClient.SCHEMATIC_AND_QUILL_HANDLER.mouseScrolled(delta) || TrainHUD.onScroll(delta)
			|| ElevatorControlsHandler.onScroll(delta);
		return cancelled;
	}

	public static boolean onMouseInput(int button, int modifiers, Action action) {
		if (MinecraftClient.getInstance().currentScreen != null)
			return false;

		boolean pressed = action == Action.PRESS;

		if (CreateClient.SCHEMATIC_HANDLER.onMouseInput(button, pressed))
			return true;
		else if (CreateClient.SCHEMATIC_AND_QUILL_HANDLER.onMouseInput(button, pressed))
			return true;
		return false;
	}

	// fabric: onClickInput split up
	public static ActionResult onUse(MinecraftClient mc, HitResult hit, Hand hand) {
		if (mc.currentScreen != null)
			return ActionResult.PASS;

		if (CurvedTrackInteraction.onClickInput(true, false)) {
			return ActionResult.SUCCESS;
		}


		boolean glueCancelled = CreateClient.GLUE_HANDLER.onMouseInput(false);
		LinkedControllerClientHandler.deactivateInLectern();
		boolean relocatorCancelled = TrainRelocator.onClicked();

		return glueCancelled || relocatorCancelled
				? ActionResult.SUCCESS
				: ActionResult.PASS;
	}

	public static ActionResult onAttack(MinecraftClient mc, HitResult hit) {
		if (mc.currentScreen != null)
			return ActionResult.PASS;

		if (CurvedTrackInteraction.onClickInput(false, true)) {
			return ActionResult.SUCCESS;
		}

		return CreateClient.GLUE_HANDLER.onMouseInput(true)
				? ActionResult.SUCCESS
				: ActionResult.PASS;
	}

	public static boolean onPick(MinecraftClient mc, HitResult hit) {
		if (mc.currentScreen != null)
			return false;

		return ToolboxHandlerClient.onPickItem();
	}

	public static void register() {
		KeyInputCallback.EVENT.register(InputEvents::onKeyInput);
		MouseInputEvents.BEFORE_SCROLL.register(InputEvents::onMouseScrolled);
		MouseInputEvents.BEFORE_BUTTON.register(InputEvents::onMouseInput);
		InteractEvents.USE.register(InputEvents::onUse);
		InteractEvents.ATTACK.register(InputEvents::onAttack);
		InteractEvents.PICK.register(InputEvents::onPick);
	}

}

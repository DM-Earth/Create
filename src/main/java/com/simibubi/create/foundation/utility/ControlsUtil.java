package com.simibubi.create.foundation.utility;

import java.util.Vector;
import com.simibubi.create.AllKeys;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class ControlsUtil {

	private static Vector<KeyBinding> standardControls;

	public static Vector<KeyBinding> getControls() {
		if (standardControls == null) {
			GameOptions gameSettings = MinecraftClient.getInstance().options;
			standardControls = new Vector<>(6);
			standardControls.add(gameSettings.forwardKey);
			standardControls.add(gameSettings.backKey);
			standardControls.add(gameSettings.leftKey);
			standardControls.add(gameSettings.rightKey);
			standardControls.add(gameSettings.jumpKey);
			standardControls.add(gameSettings.sneakKey);
		}
		return standardControls;
	}

	public static boolean isActuallyPressed(KeyBinding kb) {
		InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(kb);
		if (key.getCategory() == InputUtil.Type.MOUSE) {
			return AllKeys.isMouseButtonDown(key.getCode());
		} else {
			return AllKeys.isKeyDown(key.getCode());
		}
	}

}

package com.simibubi.create;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public enum AllKeys {

	TOOL_MENU("toolmenu", GLFW.GLFW_KEY_LEFT_ALT),
	ACTIVATE_TOOL("", GLFW.GLFW_KEY_LEFT_CONTROL),
	TOOLBELT("toolbelt", GLFW.GLFW_KEY_LEFT_ALT),

	;

	private KeyBinding keybind;
	private String description;
	private int key;
	private boolean modifiable;

	private AllKeys(String description, int defaultKey) {
		this.description = Create.ID + ".keyinfo." + description;
		this.key = defaultKey;
		this.modifiable = !description.isEmpty();
	}

	public static void register() {
		for (AllKeys key : values()) {
			if (!key.modifiable)
				continue;
			key.keybind = new KeyBinding(key.description, key.key, Create.NAME);
			KeyBindingHelper.registerKeyBinding(key.keybind);
		}
	}

	// fabric: sometimes after opening the toolbox menu, alt gets stuck as pressed until a screen is opened.
	// why is this needed? why did this only just now break? Good questions! I wish I knew.
	public static void fixBinds() {
		long window = MinecraftClient.getInstance().getWindow().getHandle();
		for (AllKeys key : values()) {
			if (key.keybind == null || key.keybind.isUnbound())
				continue;
			key.keybind.setPressed(InputUtil.isKeyPressed(window, key.getBoundCode()));
		}
	}

	public KeyBinding getKeybind() {
		return keybind;
	}

	public boolean isPressed() {
		if (!modifiable)
			return isKeyDown(key);
		return keybind.isPressed();
	}

	public String getBoundKey() {
		return keybind.getBoundKeyLocalizedText()
			.getString()
			.toUpperCase();
	}

	public int getBoundCode() {
		return KeyBindingHelper.getBoundKeyOf(keybind)
				.getCode();
	}

	public static boolean isKeyDown(int key) {
		return InputUtil.isKeyPressed(MinecraftClient.getInstance()
			.getWindow()
			.getHandle(), key);
	}

	public static boolean isMouseButtonDown(int button) {
		return GLFW.glfwGetMouseButton(MinecraftClient.getInstance()
			.getWindow()
			.getHandle(), button) == 1;
	}

	public static boolean ctrlDown() {
		return Screen.hasControlDown();
	}

	public static boolean shiftDown() {
		return Screen.hasShiftDown();
	}

	public static boolean altDown() {
		return Screen.hasAltDown();
	}

}

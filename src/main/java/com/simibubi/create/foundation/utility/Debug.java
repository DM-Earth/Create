package com.simibubi.create.foundation.utility;

import com.simibubi.create.Create;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Deprecated so simi doensn't forget to remove debug calls **/
@Environment(value = EnvType.CLIENT)
public class Debug {

	@Deprecated
	public static void debugChat(String message) {
		if (MinecraftClient.getInstance().player != null)
			MinecraftClient.getInstance().player.sendMessage(Components.literal(message), false);
	}

	@Deprecated
	public static void debugChatAndShowStack(String message, int depth) {
		if (MinecraftClient.getInstance().player != null)
			MinecraftClient.getInstance().player.sendMessage(Components.literal(message).append("@")
				.append(debugStack(depth)), false);
	}

	@Deprecated
	public static void debugMessage(String message) {
		if (MinecraftClient.getInstance().player != null)
			MinecraftClient.getInstance().player.sendMessage(Components.literal(message), true);
	}

	@Deprecated
	public static void log(String message) {
		Create.LOGGER.info(message);
	}

	@Deprecated
	public static String getLogicalSide() {
		return MinecraftClient.getInstance().world // only called on client, this is safe (but completely redundant)
			.isClient() ? "CL" : "SV";
	}

	@Deprecated
	public static Text debugStack(int depth) {
		StackTraceElement[] stackTraceElements = Thread.currentThread()
			.getStackTrace();
		MutableText text = Components.literal("[")
			.append(Components.literal(getLogicalSide()).formatted(Formatting.GOLD))
			.append("] ");
		for (int i = 1; i < depth + 2 && i < stackTraceElements.length; i++) {
			StackTraceElement e = stackTraceElements[i];
			if (e.getClassName()
				.equals(Debug.class.getName()))
				continue;
			text.append(Components.literal(e.getMethodName()).formatted(Formatting.YELLOW))
				.append(", ");
		}
		return text.append(Components.literal(" ...").formatted(Formatting.GRAY));
	}

	@Deprecated
	public static void markTemporary() {}

}

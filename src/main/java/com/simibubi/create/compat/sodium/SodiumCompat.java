package com.simibubi.create.compat.sodium;

import java.lang.reflect.Method;
import java.util.function.Function;

import com.simibubi.create.Create;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.utility.Components;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Fixes the Mechanical Saw's sprite and lets players know when Indium isn't installed.
 */
public class SodiumCompat {
	public static final Identifier SAW_TEXTURE = Create.asResource("block/saw_reversed");

	public static void init() {
		if (!Mods.INDIUM.isLoaded()) {
			ClientPlayConnectionEvents.JOIN.register(SodiumCompat::sendNoIndiumWarning);
		}
		if (spriteUtilWorks()) {
			MinecraftClient mc = MinecraftClient.getInstance();
			WorldRenderEvents.START.register(ctx -> {
				Function<Identifier, Sprite> atlas = mc.getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
				Sprite sawSprite = atlas.apply(SAW_TEXTURE);
				SpriteUtil.markSpriteActive(sawSprite);
			});
		}
	}

	public static void sendNoIndiumWarning(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient mc) {
		if (mc.player == null)
			return;

		MutableText text = Texts.bracketed(Components.literal("WARN"))
				.formatted(Formatting.GOLD)
				.append(Components.literal(" Sodium is installed, but Indium is not. This will cause visual issues with Create!")
				)
				.styled(style -> style
						.withClickEvent(
								new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/mod/indium")
						)
						.withHoverEvent(
								new HoverEvent(HoverEvent.Action.SHOW_TEXT, Components.literal("Click here to open Indium's mod page"))
						)
				);

		mc.player.sendMessage(text, false);
	}

	private static boolean spriteUtilWorks() {
		try {
			// make sure class and method still exist, sodium is unstable
			Method method = SpriteUtil.class.getMethod("markSpriteActive", Sprite.class); // throws if missing
			if (method.getReturnType() != Void.TYPE)
				throw new IllegalStateException("markSpriteActive's signature has changed");
			return true;
		} catch (Throwable t) {
			Create.LOGGER.error("Create's Sodium compat errored and has been partially disabled. Report this!", t);
		}
		return false;
	}
}

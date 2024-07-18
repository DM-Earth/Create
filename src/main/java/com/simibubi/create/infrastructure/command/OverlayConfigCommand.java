package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.utility.Components;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class OverlayConfigCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("overlay")
				.requires(cs -> cs.hasPermissionLevel(0))
				.then(CommandManager.literal("reset")
					.executes(ctx -> {
						EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> SConfigureConfigPacket.Actions.overlayReset.performAction(""));

						EnvExecutor.runWhenOn(EnvType.SERVER, () -> () ->
								AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.overlayReset.name(), ""),
										(ServerPlayerEntity) ctx.getSource().getEntity()));

					ctx.getSource()
						.sendFeedback(() -> Components.literal("reset overlay offset"), true);

						return 1;
					})
				)
				.executes(ctx -> {
					EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> SConfigureConfigPacket.Actions.overlayScreen.performAction(""));

					EnvExecutor.runWhenOn(EnvType.SERVER, () -> () ->
							AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.overlayScreen.name(), ""),
									(ServerPlayerEntity) ctx.getSource().getEntity()));

					ctx.getSource()
							.sendFeedback(() -> Components.literal("window opened"), true);

				return 1;
			});

	}
}

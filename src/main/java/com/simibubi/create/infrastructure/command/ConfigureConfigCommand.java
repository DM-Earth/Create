package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class ConfigureConfigCommand {

	protected final String commandLiteral;

	ConfigureConfigCommand(String commandLiteral) {
		this.commandLiteral = commandLiteral;
	}

	ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal(this.commandLiteral)
			.requires(cs -> cs.hasPermissionLevel(0))
			.then(CommandManager.literal("on")
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource()
						.getPlayerOrThrow();
					sendPacket(player, String.valueOf(true));

					return Command.SINGLE_SUCCESS;
				}))
			.then(CommandManager.literal("off")
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource()
						.getPlayerOrThrow();
					sendPacket(player, String.valueOf(false));

					return Command.SINGLE_SUCCESS;
				}))
			.executes(ctx -> {
				ServerPlayerEntity player = ctx.getSource()
					.getPlayerOrThrow();
				sendPacket(player, "info");

				return Command.SINGLE_SUCCESS;
			});
	}

	protected abstract void sendPacket(ServerPlayerEntity player, String option);
}

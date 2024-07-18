package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CameraDistanceCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("camera")
				.then(CommandManager.literal("reset")
						.executes(ctx -> {
							ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
							AllPackets.getChannel().sendToClient(
									new SConfigureConfigPacket(SConfigureConfigPacket.Actions.zoomMultiplier.name(), "1"),
									player
							);

							return Command.SINGLE_SUCCESS;
						})
				).then(CommandManager.argument("multiplier", FloatArgumentType.floatArg(0))
						.executes(ctx -> {
							float multiplier = FloatArgumentType.getFloat(ctx, "multiplier");
							ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
							AllPackets.getChannel().sendToClient(
									new SConfigureConfigPacket(SConfigureConfigPacket.Actions.zoomMultiplier.name(), String.valueOf(multiplier)),
									player
							);

							return Command.SINGLE_SUCCESS;
						})
				);
	}

}

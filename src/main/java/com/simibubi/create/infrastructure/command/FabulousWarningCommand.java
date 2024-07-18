package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabulousWarningCommand {

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("dismissFabulousWarning")
				.requires(AllCommands.SOURCE_IS_PLAYER)
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource()
							.getPlayerOrThrow();

					AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.fabulousWarning.name(), ""), player);

					return Command.SINGLE_SUCCESS;
				});

	}
}

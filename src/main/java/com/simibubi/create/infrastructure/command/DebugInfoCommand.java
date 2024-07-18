package com.simibubi.create.infrastructure.command;

import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.debugInfo.ServerDebugInfoPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class DebugInfoCommand {
	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return literal("debuginfo").executes(ctx -> {
			ServerCommandSource source = ctx.getSource();
			ServerPlayerEntity player = source.getPlayerOrThrow();

			Lang.translate("command.debuginfo.sending")
				.sendChat(player);
			AllPackets.getChannel()
					.sendToClient(new ServerDebugInfoPacket(player), player);

			return Command.SINGLE_SUCCESS;
		});
	}
}

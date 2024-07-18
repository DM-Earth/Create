package com.simibubi.create.infrastructure.command;

import java.util.UUID;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Components;

public class KillTrainCommand {

	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("killTrain")
			.requires(cs -> cs.hasPermissionLevel(2))
			.then(CommandManager.argument("train", UuidArgumentType.uuid())
				.executes(ctx -> {
					ServerCommandSource source = ctx.getSource();
					run(source, UuidArgumentType.getUuid(ctx, "train"));
					return 1;
				}));
	}

	private static void run(ServerCommandSource source, UUID argument) {
		Train train = Create.RAILWAYS.trains.get(argument);
		if (train == null) {
			source.sendError(Components.literal("No Train with id " + argument.toString()
				.substring(0, 5) + "[...] was found"));
			return;
		}

		train.invalid = true;
		source.sendFeedback(() -> Components.literal("Train '").append(train.name)
			.append("' removed successfully"), true);
	}

}

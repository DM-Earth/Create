package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class DebugValueCommand {

	public static float value = 0;

	public static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("debugValue")
			.requires(cs -> cs.hasPermissionLevel(4))
			.then(CommandManager.argument("value", FloatArgumentType.floatArg())
					.executes((ctx) -> {
						value = FloatArgumentType.getFloat(ctx, "value");
						ctx.getSource().sendFeedback(() -> Components.literal("Set value to: "+value), true);
						return 1;
					}));

	}
}

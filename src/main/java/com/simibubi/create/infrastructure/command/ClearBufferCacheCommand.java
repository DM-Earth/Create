package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Components;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ClearBufferCacheCommand {

	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("clearRenderBuffers")
			.requires(cs -> cs.hasPermissionLevel(0))
			.executes(ctx -> {
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> ClearBufferCacheCommand::execute);
				ctx.getSource()
					.sendFeedback(() -> Components.literal("Cleared rendering buffers."), true);
				return 1;
			});
	}

	@Environment(EnvType.CLIENT)
	private static void execute() {
		CreateClient.invalidateRenderers();
	}
}

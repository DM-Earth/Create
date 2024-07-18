package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class FixLightingCommand {

	static ArgumentBuilder<ServerCommandSource, ?> register() {
		return CommandManager.literal("fixLighting")
			.requires(cs -> cs.hasPermissionLevel(0))
			.executes(ctx -> {
				AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.fixLighting.name(), String.valueOf(true)),
						(ServerPlayerEntity) ctx.getSource().getEntity());

				ctx.getSource()
					.sendFeedback(() -> 
						Components.literal("Forge's experimental block rendering pipeline is now enabled."), true);

				return 1;
			});
	}
}

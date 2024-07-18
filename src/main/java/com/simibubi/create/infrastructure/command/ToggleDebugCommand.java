package com.simibubi.create.infrastructure.command;

import com.simibubi.create.AllPackets;
import net.minecraft.server.network.ServerPlayerEntity;

public class ToggleDebugCommand extends ConfigureConfigCommand {

	public ToggleDebugCommand() {
		super("rainbowDebug");
	}

	@Override
	protected void sendPacket(ServerPlayerEntity player, String option) {
		AllPackets.getChannel().sendToClient(new SConfigureConfigPacket(SConfigureConfigPacket.Actions.rainbowDebug.name(), option), player);
	}
}

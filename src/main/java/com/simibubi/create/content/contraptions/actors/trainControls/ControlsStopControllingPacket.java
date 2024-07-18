package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.PacketByteBuf;

public class ControlsStopControllingPacket extends SimplePacketBase {

	public ControlsStopControllingPacket() {}

	public ControlsStopControllingPacket(PacketByteBuf buffer) {}

	@Override
	public void write(PacketByteBuf buffer) {}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(ControlsHandler::stopControlling);
		return true;
	}

}

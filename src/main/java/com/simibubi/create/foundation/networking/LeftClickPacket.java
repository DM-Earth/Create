package com.simibubi.create.foundation.networking;

import com.simibubi.create.foundation.events.CommonEvents;
import net.minecraft.network.PacketByteBuf;

public class LeftClickPacket extends SimplePacketBase {

	public LeftClickPacket() {}

	public LeftClickPacket(PacketByteBuf buffer) {}

	@Override
	public void write(PacketByteBuf buffer) {}

	@Override
	public boolean handle(Context context) {
		if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER)
			return false;
		context.enqueueWork(() -> CommonEvents.leftClickEmpty(context.getSender()));
		return true;
	}

}

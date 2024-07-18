package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class ClearMenuPacket extends SimplePacketBase {

	public ClearMenuPacket() {}

	public ClearMenuPacket(PacketByteBuf buffer) {}

	@Override
	public void write(PacketByteBuf buffer) {}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;
			if (!(player.currentScreenHandler instanceof IClearableMenu))
				return;
			((IClearableMenu) player.currentScreenHandler).clearContents();
		});
		return true;
	}

}

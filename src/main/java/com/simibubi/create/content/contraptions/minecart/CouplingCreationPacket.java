package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class CouplingCreationPacket extends SimplePacketBase {

	int id1, id2;

	public CouplingCreationPacket(AbstractMinecartEntity cart1, AbstractMinecartEntity cart2) {
		id1 = cart1.getId();
		id2 = cart2.getId();
	}

	public CouplingCreationPacket(PacketByteBuf buffer) {
		id1 = buffer.readInt();
		id2 = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(id1);
		buffer.writeInt(id2);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			if (sender != null)
				CouplingHandler.tryToCoupleCarts(sender, sender.getWorld(), id1, id2);
		});
		return true;
	}

}

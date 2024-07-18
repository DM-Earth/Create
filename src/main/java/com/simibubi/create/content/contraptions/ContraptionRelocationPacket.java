package com.simibubi.create.content.contraptions;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;

public class ContraptionRelocationPacket extends SimplePacketBase {

	int entityID;

	public ContraptionRelocationPacket(int entityID) {
		this.entityID = entityID;
	}

	public ContraptionRelocationPacket(PacketByteBuf buffer) {
		entityID = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityID);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> OrientedContraptionEntity.handleRelocationPacket(this)));
		return true;
	}

}

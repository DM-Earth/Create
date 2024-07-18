package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class EjectorTriggerPacket extends BlockEntityConfigurationPacket<EjectorBlockEntity> {

	public EjectorTriggerPacket(BlockPos pos) {
		super(pos);
	}
	
	public EjectorTriggerPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {}

	@Override
	protected void readSettings(PacketByteBuf buffer) {}

	@Override
	protected void applySettings(EjectorBlockEntity be) {
		be.activate();
	}

}

package com.simibubi.create.content.kinetics.gauge;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class GaugeObservedPacket extends BlockEntityConfigurationPacket<StressGaugeBlockEntity> {

	public GaugeObservedPacket(BlockPos pos) {
		super(pos);
	}

	public GaugeObservedPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {}

	@Override
	protected void readSettings(PacketByteBuf buffer) {}

	@Override
	protected void applySettings(StressGaugeBlockEntity be) {
		be.onObserved();
	}
	
	@Override
	protected boolean causeUpdate() {
		return false;
	}

}

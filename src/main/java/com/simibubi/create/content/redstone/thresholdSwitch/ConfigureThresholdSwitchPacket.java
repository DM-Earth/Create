package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ConfigureThresholdSwitchPacket extends BlockEntityConfigurationPacket<ThresholdSwitchBlockEntity> {

	private float offBelow;
	private float onAbove;
	private boolean invert;

	public ConfigureThresholdSwitchPacket(BlockPos pos, float offBelow, float onAbove, boolean invert) {
		super(pos);
		this.offBelow = offBelow;
		this.onAbove = onAbove;
		this.invert = invert;
	}
	
	public ConfigureThresholdSwitchPacket(PacketByteBuf buffer) {
		super(buffer);
	}
	
	@Override
	protected void readSettings(PacketByteBuf buffer) {
		offBelow = buffer.readFloat();
		onAbove = buffer.readFloat();
		invert = buffer.readBoolean();
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeFloat(offBelow);
		buffer.writeFloat(onAbove);
		buffer.writeBoolean(invert);
	}

	@Override
	protected void applySettings(ThresholdSwitchBlockEntity be) {
		be.offWhenBelow = offBelow;
		be.onWhenAbove = onAbove;
		be.setInverted(invert);
	}
	
}

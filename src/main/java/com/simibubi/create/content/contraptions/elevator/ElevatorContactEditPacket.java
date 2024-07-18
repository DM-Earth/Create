package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class ElevatorContactEditPacket extends BlockEntityConfigurationPacket<ElevatorContactBlockEntity> {

	private String shortName;
	private String longName;
	private DoorControl doorControl;

	public ElevatorContactEditPacket(BlockPos pos, String shortName, String longName, DoorControl doorControl) {
		super(pos);
		this.shortName = shortName;
		this.longName = longName;
		this.doorControl = doorControl;
	}

	public ElevatorContactEditPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeSettings(PacketByteBuf buffer) {
		buffer.writeString(shortName, 4);
		buffer.writeString(longName, 30);
		buffer.writeVarInt(doorControl.ordinal());
	}

	@Override
	protected void readSettings(PacketByteBuf buffer) {
		shortName = buffer.readString(4);
		longName = buffer.readString(30);
		doorControl = DoorControl.values()[MathHelper.clamp(buffer.readVarInt(), 0, DoorControl.values().length)];
	}

	@Override
	protected void applySettings(ElevatorContactBlockEntity be) {
		be.updateName(shortName, longName);
		be.doorControls.set(doorControl);
	}

}

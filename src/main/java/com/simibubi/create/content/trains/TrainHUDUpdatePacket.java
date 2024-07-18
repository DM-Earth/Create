package com.simibubi.create.content.trains;

import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.networking.SimplePacketBase;

public class TrainHUDUpdatePacket extends SimplePacketBase {

	UUID trainId;

	Double throttle;
	double speed;
	int fuelTicks;

	public TrainHUDUpdatePacket() {}

	public TrainHUDUpdatePacket(Train train) {
		trainId = train.id;
		throttle = train.throttle;
		speed = train.speedBeforeStall == null ? train.speed : train.speedBeforeStall;
		fuelTicks = train.fuelTicks;
	}

	public TrainHUDUpdatePacket(PacketByteBuf buffer) {
		trainId = buffer.readUuid();
		if (buffer.readBoolean())
			throttle = buffer.readDouble();
		speed = buffer.readDouble();
		fuelTicks = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeUuid(trainId);
		buffer.writeBoolean(throttle != null);
		if (throttle != null)
			buffer.writeDouble(throttle);
		buffer.writeDouble(speed);
		buffer.writeInt(fuelTicks);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			boolean clientSide = sender == null;
			Train train = Create.RAILWAYS.sided(clientSide ? null : sender.getWorld()).trains.get(trainId);
			if (train == null)
				return;

			if (throttle != null)
				train.throttle = throttle;
			if (clientSide) {
				train.speed = speed;
				train.fuelTicks = fuelTicks;
			}
		});
		return true;
	}

	public static class Serverbound extends TrainHUDUpdatePacket {

		public Serverbound(PacketByteBuf buffer) {
			super(buffer);
		}

		public Serverbound(Train train, Double sendThrottle) {
			trainId = train.id;
			throttle = sendThrottle;
		}
	}

}

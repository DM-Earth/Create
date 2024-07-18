package com.simibubi.create.content.trains.entity;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;

public class CarriageDataUpdatePacket extends SimplePacketBase {

	private int entity;
	private CarriageSyncData data;

	public CarriageDataUpdatePacket(CarriageContraptionEntity entity) {
		this.entity = entity.getId();
		this.data = entity.carriageData;
	}

	public CarriageDataUpdatePacket(PacketByteBuf buf) {
		this.entity = buf.readVarInt();
		this.data = new CarriageSyncData();
		this.data.read(buf);
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeVarInt(entity);
		this.data.write(buffer);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			Entity entity = mc.world.getEntityById(this.entity);
			if (entity instanceof CarriageContraptionEntity carriage) {
				carriage.onCarriageDataUpdate(this.data);
			} else {
				Create.LOGGER.error("Invalid CarriageDataUpdatePacket for non-carriage entity: " + entity);
			}
		});
		return true;
	}
}

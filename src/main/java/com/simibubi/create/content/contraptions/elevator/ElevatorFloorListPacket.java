package com.simibubi.create.content.contraptions.elevator;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.IntAttached;

public class ElevatorFloorListPacket extends SimplePacketBase {

	private int entityId;
	private List<IntAttached<Couple<String>>> floorsList;

	public ElevatorFloorListPacket(AbstractContraptionEntity entity, List<IntAttached<Couple<String>>> floorsList) {
		this.entityId = entity.getId();
		this.floorsList = floorsList;
	}

	public ElevatorFloorListPacket(PacketByteBuf buffer) {
		entityId = buffer.readInt();
		int size = buffer.readInt();
		floorsList = new ArrayList<>();
		for (int i = 0; i < size; i++)
			floorsList.add(IntAttached.with(buffer.readInt(), Couple.create(buffer.readString(), buffer.readString())));
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeInt(floorsList.size());
		for (IntAttached<Couple<String>> entry : floorsList) {
			buffer.writeInt(entry.getFirst());
			entry.getSecond()
				.forEach(buffer::writeString);
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityId);
			if (!(entityByID instanceof AbstractContraptionEntity ace))
				return;
			if (!(ace.getContraption()instanceof ElevatorContraption ec))
				return;

			ec.namesList = floorsList;
			ec.syncControlDisplays();
		});
		return true;
	}

	public static class RequestFloorList extends SimplePacketBase {

		private int entityId;

		public RequestFloorList(AbstractContraptionEntity entity) {
			this.entityId = entity.getId();
		}

		public RequestFloorList(PacketByteBuf buffer) {
			entityId = buffer.readInt();
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer.writeInt(entityId);
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> {
				ServerPlayerEntity sender = context.getSender();
				Entity entityByID = sender.getWorld()
					.getEntityById(entityId);
				if (!(entityByID instanceof AbstractContraptionEntity ace))
					return;
				if (!(ace.getContraption()instanceof ElevatorContraption ec))
					return;
				AllPackets.getChannel().sendToClient(new ElevatorFloorListPacket(ace, ec.namesList), sender);
			});
			return true;
		}

	}

}

package com.simibubi.create.content.trains.station;

import java.util.UUID;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.Components;

public class TrainEditPacket extends SimplePacketBase {

	private String name;
	private UUID id;
	private Identifier iconType;

	public TrainEditPacket(UUID id, String name, Identifier iconType) {
		this.name = name;
		this.id = id;
		this.iconType = iconType;
	}

	public TrainEditPacket(PacketByteBuf buffer) {
		id = buffer.readUuid();
		name = buffer.readString(256);
		iconType = buffer.readIdentifier();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeUuid(id);
		buffer.writeString(name);
		buffer.writeIdentifier(iconType);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			World level = sender == null ? null : sender.getWorld();
			Train train = Create.RAILWAYS.sided(level).trains.get(id);
			if (train == null)
				return;
			if (!name.isBlank())
				train.name = Components.literal(name);
			train.icon = TrainIconType.byId(iconType);
			if (sender != null)
				AllPackets.getChannel().sendToClientsInServer(new TrainEditReturnPacket(id, name, iconType),
						level.getServer());
		});
		return true;
	}

	public static class TrainEditReturnPacket extends TrainEditPacket {

		public TrainEditReturnPacket(PacketByteBuf buffer) {
			super(buffer);
		}

		public TrainEditReturnPacket(UUID id, String name, Identifier iconType) {
			super(id, name, iconType);
		}

	}

}

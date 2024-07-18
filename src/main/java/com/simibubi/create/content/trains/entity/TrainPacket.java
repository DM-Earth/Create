package com.simibubi.create.content.trains.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.RegisteredObjects;

public class TrainPacket extends SimplePacketBase {

	UUID trainId;
	Train train;
	boolean add;

	public TrainPacket(Train train, boolean add) {
		this.train = train;
		this.add = add;
	}

	public TrainPacket(PacketByteBuf buffer) {
		add = buffer.readBoolean();
		trainId = buffer.readUuid();

		if (!add)
			return;

		UUID owner = null;
		if (buffer.readBoolean())
			owner = buffer.readUuid();

		List<Carriage> carriages = new ArrayList<>();
		List<Integer> carriageSpacing = new ArrayList<>();

		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++) {
			Couple<CarriageBogey> bogies = Couple.create(null, null);
			for (boolean isFirst : Iterate.trueAndFalse) {
				if (!isFirst && !buffer.readBoolean())
					continue;
				AbstractBogeyBlock<?> type = (AbstractBogeyBlock<?>) Registries.BLOCK.get(buffer.readIdentifier());
				boolean upsideDown = buffer.readBoolean();
				NbtCompound data = buffer.readNbt();
				bogies.set(isFirst, new CarriageBogey(type, upsideDown, data, new TravellingPoint(), new TravellingPoint()));
			}
			int spacing = buffer.readVarInt();
			carriages.add(new Carriage(bogies.getFirst(), bogies.getSecond(), spacing));
		}

		size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			carriageSpacing.add(buffer.readVarInt());

		boolean doubleEnded = buffer.readBoolean();
		train = new Train(trainId, owner, null, carriages, carriageSpacing, doubleEnded);

		train.name = Text.Serializer.fromJson(buffer.readString());
		train.icon = TrainIconType.byId(buffer.readIdentifier());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBoolean(add);
		buffer.writeUuid(train.id);

		if (!add)
			return;

		buffer.writeBoolean(train.owner != null);
		if (train.owner != null)
			buffer.writeUuid(train.owner);

		buffer.writeVarInt(train.carriages.size());
		for (Carriage carriage : train.carriages) {
			for (boolean first : Iterate.trueAndFalse) {
				if (!first) {
					boolean onTwoBogeys = carriage.isOnTwoBogeys();
					buffer.writeBoolean(onTwoBogeys);
					if (!onTwoBogeys)
						continue;
				}
				CarriageBogey bogey = carriage.bogeys.get(first);
				buffer.writeIdentifier(RegisteredObjects.getKeyOrThrow((Block) bogey.type));
				buffer.writeBoolean(bogey.upsideDown);
				buffer.writeNbt(bogey.bogeyData);
			}
			buffer.writeVarInt(carriage.bogeySpacing);
		}

		buffer.writeVarInt(train.carriageSpacing.size());
		train.carriageSpacing.forEach(buffer::writeVarInt);

		buffer.writeBoolean(train.doubleEnded);
		buffer.writeString(Text.Serializer.toJson(train.name));
		buffer.writeIdentifier(train.icon.id);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Map<UUID, Train> trains = CreateClient.RAILWAYS.trains;
			if (add)
				trains.put(train.id, train);
			else
				trains.remove(trainId);
		});
		return true;
	}

}

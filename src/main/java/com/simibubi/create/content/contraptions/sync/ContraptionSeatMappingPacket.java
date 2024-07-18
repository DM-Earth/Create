package com.simibubi.create.content.contraptions.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.VecHelper;

public class ContraptionSeatMappingPacket extends SimplePacketBase {

	private Map<UUID, Integer> mapping;
	private int entityID;
	private int dismountedID;

	public ContraptionSeatMappingPacket(int entityID, Map<UUID, Integer> mapping) {
		this(entityID, mapping, -1);
	}

	public ContraptionSeatMappingPacket(int entityID, Map<UUID, Integer> mapping, int dismountedID) {
		this.entityID = entityID;
		this.mapping = mapping;
		this.dismountedID = dismountedID;
	}

	public ContraptionSeatMappingPacket(PacketByteBuf buffer) {
		entityID = buffer.readInt();
		dismountedID = buffer.readInt();
		mapping = new HashMap<>();
		short size = buffer.readShort();
		for (int i = 0; i < size; i++)
			mapping.put(buffer.readUuid(), (int) buffer.readShort());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeInt(dismountedID);
		buffer.writeShort(mapping.size());
		mapping.forEach((k, v) -> {
			buffer.writeUuid(k);
			buffer.writeShort(v);
		});
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
				Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityID);
				if (!(entityByID instanceof AbstractContraptionEntity))
					return;
				AbstractContraptionEntity contraptionEntity = (AbstractContraptionEntity) entityByID;

				if (dismountedID != -1) {
					Entity dismountedByID = MinecraftClient.getInstance().world.getEntityById(dismountedID);
					if (MinecraftClient.getInstance().player != dismountedByID)
						return;
					Vec3d transformedVector = contraptionEntity.getPassengerPosition(dismountedByID, 1);
					if (transformedVector != null)
						dismountedByID.getCustomData()
							.put("ContraptionDismountLocation", VecHelper.writeNBT(transformedVector));
				}

				contraptionEntity.getContraption()
					.setSeatMapping(mapping);
			});
		return true;
	}

}

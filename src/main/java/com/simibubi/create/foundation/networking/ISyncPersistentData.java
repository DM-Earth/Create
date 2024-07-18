package com.simibubi.create.foundation.networking;

import java.util.HashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import com.simibubi.create.AllPackets;

public interface ISyncPersistentData {

	void onPersistentDataUpdated();

	default void syncPersistentDataWithTracking(Entity self) {
		AllPackets.getChannel().sendToClientsTracking(new PersistentDataPacket(self), self);
	}

	public static class PersistentDataPacket extends SimplePacketBase {

		private int entityId;
		private Entity entity;
		private NbtCompound readData;

		public PersistentDataPacket(Entity entity) {
			this.entity = entity;
			this.entityId = entity.getId();
		}

		public PersistentDataPacket(PacketByteBuf buffer) {
			entityId = buffer.readInt();
			readData = buffer.readNbt();
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer.writeInt(entityId);
			buffer.writeNbt(entity.getCustomData());
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> {
				Entity entityByID = MinecraftClient.getInstance().world.getEntityById(entityId);
				NbtCompound data = entityByID.getCustomData();
				new HashSet<>(data.getKeys()).forEach(data::remove);
				data.copyFrom(readData);
				if (!(entityByID instanceof ISyncPersistentData))
					return;
				((ISyncPersistentData) entityByID).onPersistentDataUpdated();
			});
			return true;
		}

	}

}

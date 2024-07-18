package com.simibubi.create.content.contraptions.sync;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public class LimbSwingUpdatePacket extends SimplePacketBase {

	private int entityId;
	private Vec3d position;
	private float limbSwing;

	public LimbSwingUpdatePacket(int entityId, Vec3d position, float limbSwing) {
		this.entityId = entityId;
		this.position = position;
		this.limbSwing = limbSwing;
	}

	public LimbSwingUpdatePacket(PacketByteBuf buffer) {
		entityId = buffer.readInt();
		position = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		limbSwing = buffer.readFloat();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeDouble(position.x);
		buffer.writeDouble(position.y);
		buffer.writeDouble(position.z);
		buffer.writeFloat(limbSwing);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ClientWorld world = MinecraftClient.getInstance().world;
			if (world == null)
				return;
			Entity entity = world.getEntityById(entityId);
			if (entity == null)
				return;
			NbtCompound data = entity.getCustomData();
			data.putInt("LastOverrideLimbSwingUpdate", 0);
			data.putFloat("OverrideLimbSwing", limbSwing);
			entity.updateTrackedPositionAndAngles(position.x, position.y, position.z, entity.getYaw(),
				entity.getPitch(), 2, false);
		});
		return true;
	}

}

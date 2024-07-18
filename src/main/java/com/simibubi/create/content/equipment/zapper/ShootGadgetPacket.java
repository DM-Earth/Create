package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public abstract class ShootGadgetPacket extends SimplePacketBase {

	public Vec3d location;
	public Hand hand;
	public boolean self;

	public ShootGadgetPacket(Vec3d location, Hand hand, boolean self) {
		this.location = location;
		this.hand = hand;
		this.self = self;
	}

	public ShootGadgetPacket(PacketByteBuf buffer) {
		hand = buffer.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
		self = buffer.readBoolean();
		location = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		readAdditional(buffer);
	}

	public final void write(PacketByteBuf buffer) {
		buffer.writeBoolean(hand == Hand.MAIN_HAND);
		buffer.writeBoolean(self);
		buffer.writeDouble(location.x);
		buffer.writeDouble(location.y);
		buffer.writeDouble(location.z);
		writeAdditional(buffer);
	}

	protected abstract void readAdditional(PacketByteBuf buffer);

	protected abstract void writeAdditional(PacketByteBuf buffer);

	@Environment(EnvType.CLIENT)
	protected abstract void handleAdditional();

	@Environment(EnvType.CLIENT)
	protected abstract ShootableGadgetRenderHandler getHandler();

	@Override
	@Environment(EnvType.CLIENT)
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Entity renderViewEntity = MinecraftClient.getInstance()
				.getCameraEntity();
			if (renderViewEntity == null)
				return;
			if (renderViewEntity.getPos()
				.distanceTo(location) > 100)
				return;

			ShootableGadgetRenderHandler handler = getHandler();
			handleAdditional();
			if (self)
				handler.shoot(hand, location);
			else
				handler.playSound(hand, location);
		});
		return true;
	}

}

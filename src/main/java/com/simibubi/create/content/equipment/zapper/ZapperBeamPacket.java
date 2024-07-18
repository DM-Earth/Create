package com.simibubi.create.content.equipment.zapper;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler.LaserBeam;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ZapperBeamPacket extends ShootGadgetPacket {

	public Vec3d target;

	public ZapperBeamPacket(Vec3d start, Vec3d target, Hand hand, boolean self) {
		super(start, hand, self);
		this.target = target;
	}

	public ZapperBeamPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void readAdditional(PacketByteBuf buffer) {
		target = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	@Override
	protected void writeAdditional(PacketByteBuf buffer) {
		buffer.writeDouble(target.x);
		buffer.writeDouble(target.y);
		buffer.writeDouble(target.z);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected ShootableGadgetRenderHandler getHandler() {
		return CreateClient.ZAPPER_RENDER_HANDLER;
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void handleAdditional() {
		CreateClient.ZAPPER_RENDER_HANDLER.addBeam(new LaserBeam(location, target));
	}

}

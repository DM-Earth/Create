package com.simibubi.create.content.equipment.potatoCannon;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.zapper.ShootGadgetPacket;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class PotatoCannonPacket extends ShootGadgetPacket {

	private float pitch;
	private Vec3d motion;
	private ItemStack item;

	public PotatoCannonPacket(Vec3d location, Vec3d motion, ItemStack item, Hand hand, float pitch, boolean self) {
		super(location, hand, self);
		this.motion = motion;
		this.item = item;
		this.pitch = pitch;
	}

	public PotatoCannonPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void readAdditional(PacketByteBuf buffer) {
		pitch = buffer.readFloat();
		motion = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		item = buffer.readItemStack();
	}

	@Override
	protected void writeAdditional(PacketByteBuf buffer) {
		buffer.writeFloat(pitch);
		buffer.writeFloat((float) motion.x);
		buffer.writeFloat((float) motion.y);
		buffer.writeFloat((float) motion.z);
		buffer.writeItemStack(item);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void handleAdditional() {
		CreateClient.POTATO_CANNON_RENDER_HANDLER.beforeShoot(pitch, location, motion, item);
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected ShootableGadgetRenderHandler getHandler() {
		return CreateClient.POTATO_CANNON_RENDER_HANDLER;
	}

}

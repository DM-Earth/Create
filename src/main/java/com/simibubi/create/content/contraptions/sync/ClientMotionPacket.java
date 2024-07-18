package com.simibubi.create.content.contraptions.sync;

import java.util.function.Supplier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.foundation.mixin.fabric.ServerGamePacketListenerImplAccessor;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.SimplePacketBase;

public class ClientMotionPacket extends SimplePacketBase {

	private Vec3d motion;
	private boolean onGround;
	private float limbSwing;

	public ClientMotionPacket(Vec3d motion, boolean onGround, float limbSwing) {
		this.motion = motion;
		this.onGround = onGround;
		this.limbSwing = limbSwing;
	}

	public ClientMotionPacket(PacketByteBuf buffer) {
		motion = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		onGround = buffer.readBoolean();
		limbSwing = buffer.readFloat();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeFloat((float) motion.x);
		buffer.writeFloat((float) motion.y);
		buffer.writeFloat((float) motion.z);
		buffer.writeBoolean(onGround);
		buffer.writeFloat(limbSwing);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			if (sender == null)
				return;
			sender.setVelocity(motion);
			sender.setOnGround(onGround);
			if (onGround) {
				sender.handleFallDamage(sender.fallDistance, 1, sender.getDamageSources().fall());
				sender.fallDistance = 0;
				ServerGamePacketListenerImplAccessor access = (ServerGamePacketListenerImplAccessor) sender.networkHandler;
					access.create$setFloatingTicks(0);
				access.create$setVehicleFloatingTicks(0);
			}
			AllPackets.getChannel().sendToClientsTracking(new LimbSwingUpdatePacket(sender.getId(), sender.getPos(), limbSwing), sender);
		});
		return true;
	}

}

package com.simibubi.create.content.equipment.bell;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class SoulPulseEffectPacket extends SimplePacketBase {

	public BlockPos pos;
	public int distance;
	public boolean canOverlap;

	public SoulPulseEffectPacket(BlockPos pos, int distance, boolean overlaps) {
		this.pos = pos;
		this.distance = distance;
		this.canOverlap = overlaps;
	}

	public SoulPulseEffectPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		distance = buffer.readInt();
		canOverlap = buffer.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeInt(distance);
		buffer.writeBoolean(canOverlap);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			CreateClient.SOUL_PULSE_EFFECT_HANDLER.addPulse(new SoulPulseEffect(pos, distance, canOverlap));
		});
		return true;
	}

}

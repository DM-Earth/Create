package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.AdventureUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class SuperGlueRemovalPacket extends SimplePacketBase {

	private int entityId;
	private BlockPos soundSource;

	public SuperGlueRemovalPacket(int id, BlockPos soundSource) {
		entityId = id;
		this.soundSource = soundSource;
	}

	public SuperGlueRemovalPacket(PacketByteBuf buffer) {
		entityId = buffer.readInt();
		soundSource = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeBlockPos(soundSource);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (AdventureUtil.isAdventure(player))
				return;
			Entity entity = player.getWorld().getEntityById(entityId);
			if (!(entity instanceof SuperGlueEntity superGlue))
				return;
			double range = 32;
			if (player.squaredDistanceTo(superGlue.getPos()) > range * range)
				return;
			AllSoundEvents.SLIME_ADDED.play(player.getWorld(), null, soundSource, 0.5F, 0.5F);
			superGlue.spawnParticles();
			entity.discard();
		});
		return true;
	}

}

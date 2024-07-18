package com.simibubi.create.content.contraptions;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class TrainCollisionPacket extends SimplePacketBase {

	int damage;
	int contraptionEntityId;

	public TrainCollisionPacket(int damage, int contraptionEntityId) {
		this.damage = damage;
		this.contraptionEntityId = contraptionEntityId;
	}

	public TrainCollisionPacket(PacketByteBuf buffer) {
		contraptionEntityId = buffer.readInt();
		damage = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(contraptionEntityId);
		buffer.writeInt(damage);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			World level = player.getWorld();

			Entity entity = level.getEntityById(contraptionEntityId);
			if (!(entity instanceof CarriageContraptionEntity cce))
				return;

			player.damage(CreateDamageSources.runOver(level, cce), damage);
			player.getWorld().playSound(player, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL,
				1, .75f);
		});
		return true;
	}

}

package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ElevatorTargetFloorPacket extends SimplePacketBase {

	private int entityId;
	private int targetY;

	public ElevatorTargetFloorPacket(AbstractContraptionEntity entity, int targetY) {
		this.targetY = targetY;
		this.entityId = entity.getId();
	}

	public ElevatorTargetFloorPacket(PacketByteBuf buffer) {
		entityId = buffer.readInt();
		targetY = buffer.readInt();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityId);
		buffer.writeInt(targetY);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity sender = context.getSender();
			Entity entityByID = sender.getServerWorld()
				.getEntityById(entityId);
			if (!(entityByID instanceof AbstractContraptionEntity ace))
				return;
			if (!(ace.getContraption() instanceof ElevatorContraption ec))
				return;
			if (ace.squaredDistanceTo(sender) > 50 * 50)
				return;

			World level = sender.getWorld();
			ElevatorColumn elevatorColumn = ElevatorColumn.get(level, ec.getGlobalColumn());
			if (!elevatorColumn.contacts.contains(targetY))
				return;
			if (ec.isTargetUnreachable(targetY))
				return;

			BlockPos pos = elevatorColumn.contactAt(targetY);
			BlockState blockState = level.getBlockState(pos);
			if (!(blockState.getBlock() instanceof ElevatorContactBlock ecb))
				return;

			ecb.callToContactAndUpdate(elevatorColumn, blockState, level, pos, false);
		});
		return true;
	}

}

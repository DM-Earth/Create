package com.simibubi.create.content.contraptions.actors.trainControls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;

public class ControlsInputPacket extends SimplePacketBase {

	private Collection<Integer> activatedButtons;
	private boolean press;
	private int contraptionEntityId;
	private BlockPos controlsPos;
	private boolean stopControlling;

	public ControlsInputPacket(Collection<Integer> activatedButtons, boolean press, int contraptionEntityId,
		BlockPos controlsPos, boolean stopControlling) {
		this.contraptionEntityId = contraptionEntityId;
		this.activatedButtons = activatedButtons;
		this.press = press;
		this.controlsPos = controlsPos;
		this.stopControlling = stopControlling;
	}

	public ControlsInputPacket(PacketByteBuf buffer) {
		contraptionEntityId = buffer.readInt();
		activatedButtons = new ArrayList<>();
		press = buffer.readBoolean();
		int size = buffer.readVarInt();
		for (int i = 0; i < size; i++)
			activatedButtons.add(buffer.readVarInt());
		controlsPos = buffer.readBlockPos();
		stopControlling = buffer.readBoolean();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(contraptionEntityId);
		buffer.writeBoolean(press);
		buffer.writeVarInt(activatedButtons.size());
		activatedButtons.forEach(buffer::writeVarInt);
		buffer.writeBlockPos(controlsPos);
		buffer.writeBoolean(stopControlling);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			World world = player.getEntityWorld();
			UUID uniqueID = player.getUuid();

			if (player.isSpectator() && press)
				return;

			Entity entity = world.getEntityById(contraptionEntityId);
			if (!(entity instanceof AbstractContraptionEntity ace))
				return;
			if (stopControlling) {
				ace.stopControlling(controlsPos);
				return;
			}

			if (ace.toGlobalVector(Vec3d.ofCenter(controlsPos), 0)
				.isInRange(player.getPos(), 16))
				ControlsServerHandler.receivePressed(world, ace, controlsPos, uniqueID, activatedButtons, press);
		});
		return true;
	}

}

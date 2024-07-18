package com.simibubi.create.compat.computercraft;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class AttachedComputerPacket extends BlockEntityDataPacket<SyncedBlockEntity> {

	private final boolean hasAttachedComputer;

	public AttachedComputerPacket(BlockPos blockEntityPos, boolean hasAttachedComputer) {
		super(blockEntityPos);
		this.hasAttachedComputer = hasAttachedComputer;
	}

	public AttachedComputerPacket(PacketByteBuf buffer) {
		super(buffer);
		this.hasAttachedComputer = buffer.readBoolean();
	}

	@Override
	protected void writeData(PacketByteBuf buffer) {
		buffer.writeBoolean(hasAttachedComputer);
	}

	@Override
	protected void handlePacket(SyncedBlockEntity blockEntity) {
		if (blockEntity instanceof SmartBlockEntity sbe) {
			sbe.getBehaviour(AbstractComputerBehaviour.TYPE)
				.setHasAttachedComputer(hasAttachedComputer);
		}
	}

}

package com.simibubi.create.foundation.blockEntity;

import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class RemoveBlockEntityPacket extends BlockEntityDataPacket<SyncedBlockEntity> {

	public RemoveBlockEntityPacket(BlockPos pos) {
		super(pos);
	}

	public RemoveBlockEntityPacket(PacketByteBuf buffer) {
		super(buffer);
	}

	@Override
	protected void writeData(PacketByteBuf buffer) {}

	@Override
	protected void handlePacket(SyncedBlockEntity be) {
		if (!be.hasWorld()) {
			be.markRemoved();
			return;
		}

		be.getWorld()
			.removeBlockEntity(pos);
	}

}

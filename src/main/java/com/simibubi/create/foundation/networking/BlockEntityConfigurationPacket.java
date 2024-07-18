package com.simibubi.create.foundation.networking;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class BlockEntityConfigurationPacket<BE extends SyncedBlockEntity> extends SimplePacketBase {

	protected BlockPos pos;

	public BlockEntityConfigurationPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
		readSettings(buffer);
	}

	public BlockEntityConfigurationPacket(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		writeSettings(buffer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayerEntity player = context.getSender();
			if (player == null)
				return;
			World world = player.getWorld();
			if (world == null || !world.canSetBlock(pos))
				return;
			if (!pos.isWithinDistance(player.getBlockPos(), maxRange()))
				return;
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof SyncedBlockEntity) {
				applySettings(player, (BE) blockEntity);
				if (!causeUpdate())
					return;
				((SyncedBlockEntity) blockEntity).sendData();
				blockEntity.markDirty();
			}
		});
		return true;
	}

	protected int maxRange() {
		return 20;
	}

	protected abstract void writeSettings(PacketByteBuf buffer);

	protected abstract void readSettings(PacketByteBuf buffer);

	protected void applySettings(ServerPlayerEntity player, BE be) {
		applySettings(be);
	}

	protected boolean causeUpdate() {
		return true;
	}

	protected abstract void applySettings(BE be);

}

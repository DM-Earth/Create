package com.simibubi.create.foundation.networking;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * A server to client version of {@link BlockEntityConfigurationPacket}
 *
 * @param <BE>
 */
public abstract class BlockEntityDataPacket<BE extends SyncedBlockEntity> extends SimplePacketBase {

	protected BlockPos pos;

	public BlockEntityDataPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
	}

	public BlockEntityDataPacket(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
		writeData(buffer);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ClientWorld world = MinecraftClient.getInstance().world;

			if (world == null)
				return;

			BlockEntity blockEntity = world.getBlockEntity(pos);

			if (blockEntity instanceof SyncedBlockEntity) {
				handlePacket((BE) blockEntity);
			}
		});
		return true;
	}

	protected abstract void writeData(PacketByteBuf buffer);

	protected abstract void handlePacket(BE blockEntity);
}

package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class EjectorElytraPacket extends SimplePacketBase {

	private BlockPos pos;

	public EjectorElytraPacket(BlockPos pos) {
		this.pos = pos;
	}

	public EjectorElytraPacket(PacketByteBuf buffer) {
		pos = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeBlockPos(pos);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
					ServerPlayerEntity player = context.getSender();
					if (player == null)
						return;
					World world = player.getWorld();
					if (world == null || !world.canSetBlock(pos))
						return;
					BlockEntity blockEntity = world.getBlockEntity(pos);
					if (blockEntity instanceof EjectorBlockEntity)
						((EjectorBlockEntity) blockEntity).deployElytra(player);
				});
		return true;
	}

}

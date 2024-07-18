package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class EjectorPlacementPacket extends SimplePacketBase {

	private int h, v;
	private BlockPos pos;
	private Direction facing;

	public EjectorPlacementPacket(int h, int v, BlockPos pos, Direction facing) {
		this.h = h;
		this.v = v;
		this.pos = pos;
		this.facing = facing;
	}

	public EjectorPlacementPacket(PacketByteBuf buffer) {
		h = buffer.readInt();
		v = buffer.readInt();
		pos = buffer.readBlockPos();
		facing = Direction.byId(buffer.readVarInt());
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(h);
		buffer.writeInt(v);
		buffer.writeBlockPos(pos);
		buffer.writeVarInt(facing.getId());
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
			BlockState state = world.getBlockState(pos);
			if (blockEntity instanceof EjectorBlockEntity)
				((EjectorBlockEntity) blockEntity).setTarget(h, v);
			if (AllBlocks.WEIGHTED_EJECTOR.has(state))
				world.setBlockState(pos, state.with(EjectorBlock.HORIZONTAL_FACING, facing));
		});
		return true;
	}

	public static class ClientBoundRequest extends SimplePacketBase {

		BlockPos pos;

		public ClientBoundRequest(BlockPos pos) {
			this.pos = pos;
		}

		public ClientBoundRequest(PacketByteBuf buffer) {
			this.pos = buffer.readBlockPos();
		}

		@Override
		public void write(PacketByteBuf buffer) {
			buffer.writeBlockPos(pos);
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(
				() -> EnvExecutor.runWhenOn(EnvType.CLIENT,
						() -> () -> EjectorTargetHandler.flushSettings(pos)));
			return true;
		}

	}

}

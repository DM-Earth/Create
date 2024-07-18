package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.Collection;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ArmPlacementPacket extends SimplePacketBase {

	private Collection<ArmInteractionPoint> points;
	private NbtList receivedTag;
	private BlockPos pos;

	public ArmPlacementPacket(Collection<ArmInteractionPoint> points, BlockPos pos) {
		this.points = points;
		this.pos = pos;
	}

	public ArmPlacementPacket(PacketByteBuf buffer) {
		NbtCompound nbt = buffer.readNbt();
		receivedTag = nbt.getList("Points", NbtElement.COMPOUND_TYPE);
		pos = buffer.readBlockPos();
	}

	@Override
	public void write(PacketByteBuf buffer) {
		NbtCompound nbt = new NbtCompound();
		NbtList pointsNBT = new NbtList();
		points.stream()
			.map(aip -> aip.serialize(pos))
			.forEach(pointsNBT::add);
		nbt.put("Points", pointsNBT);
		buffer.writeNbt(nbt);
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
			if (!(blockEntity instanceof ArmBlockEntity))
				return;

			ArmBlockEntity arm = (ArmBlockEntity) blockEntity;
			arm.interactionPointTag = receivedTag;
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
			context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT,
				() -> () -> ArmInteractionPointHandler.flushSettings(pos)));
			return true;
		}

	}

}

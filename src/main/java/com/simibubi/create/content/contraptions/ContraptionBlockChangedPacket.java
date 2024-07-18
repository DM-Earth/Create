package com.simibubi.create.content.contraptions;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

public class ContraptionBlockChangedPacket extends SimplePacketBase {

	int entityID;
	BlockPos localPos;
	BlockState newState;

	public ContraptionBlockChangedPacket(int id, BlockPos pos, BlockState state) {
		entityID = id;
		localPos = pos;
		newState = state;
	}

	@Override
	public void write(PacketByteBuf buffer) {
		buffer.writeInt(entityID);
		buffer.writeBlockPos(localPos);
		buffer.writeNbt(NbtHelper.fromBlockState(newState));
	}

	@SuppressWarnings("deprecation")
	public ContraptionBlockChangedPacket(PacketByteBuf buffer) {
		entityID = buffer.readInt();
		localPos = buffer.readBlockPos();
		newState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), buffer.readNbt());
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT,
			() -> () -> AbstractContraptionEntity.handleBlockChangedPacket(this)));
		return true;
	}

}

package com.simibubi.create.foundation.blockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import io.github.fabricators_of_create.porting_lib.block.CustomDataPacketHandlingBlockEntity;
import io.github.fabricators_of_create.porting_lib.block.CustomUpdateTagHandlingBlockEntity;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class SyncedBlockEntity extends BlockEntity implements CustomDataPacketHandlingBlockEntity, CustomUpdateTagHandlingBlockEntity {

	public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return writeClient(new NbtCompound());
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public void handleUpdateTag(NbtCompound tag) {
		readClient(tag);
	}

	@Override
	public void onDataPacket(ClientConnection connection, BlockEntityUpdateS2CPacket packet) {
		NbtCompound tag = packet.getNbt();
		readClient(tag == null ? new NbtCompound() : tag);
	}

	// Special handling for client update packets
	public void readClient(NbtCompound tag) {
		readNbt(tag);
	}

	// Special handling for client update packets
	public NbtCompound writeClient(NbtCompound tag) {
		writeNbt(tag);
		return tag;
	}

	public void sendData() {
		if (world instanceof ServerWorld serverLevel)
			serverLevel.getChunkManager().markForUpdate(getPos());
	}

	public void notifyUpdate() {
		markDirty();
		sendData();
	}

//	public PacketDistributor.PacketTarget packetTarget() {
//		return PacketDistributor.TRACKING_CHUNK.with(this::containedChunk);
//	}

	public WorldChunk containedChunk() {
		return world.getWorldChunk(pos);
	}

	@Override
	public void deserializeNBT(BlockState state, NbtCompound nbt) {
		this.readNbt(nbt);
	}

	@SuppressWarnings("deprecation")
	public RegistryEntryLookup<Block> blockHolderGetter() {
		return (RegistryEntryLookup<Block>) (world != null ? world.createCommandRegistryWrapper(RegistryKeys.BLOCK)
			: Registries.BLOCK.getReadOnlyWrapper());
	}

}

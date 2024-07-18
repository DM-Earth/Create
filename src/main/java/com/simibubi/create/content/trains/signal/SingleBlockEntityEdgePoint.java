package com.simibubi.create.content.trains.signal;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class SingleBlockEntityEdgePoint extends TrackEdgePoint {

	public RegistryKey<World> blockEntityDimension;
	public BlockPos blockEntityPos;

	public BlockPos getBlockEntityPos() {
		return blockEntityPos;
	}
	
	public RegistryKey<World> getBlockEntityDimension() {
		return blockEntityDimension;
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		this.blockEntityPos = blockEntity.getPos();
		this.blockEntityDimension = blockEntity.getWorld()
			.getRegistryKey();
	}

	@Override
	public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
		removeFromAllGraphs();
	}

	@Override
	public void invalidate(WorldAccess level) {
		invalidateAt(level, blockEntityPos);
	}

	@Override
	public boolean canMerge() {
		return false;
	}

	@Override
	public void read(NbtCompound nbt, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, migration, dimensions);
		if (migration)
			return;
		blockEntityPos = NbtHelper.toBlockPos(nbt.getCompound("TilePos"));
		blockEntityDimension = dimensions.decode(nbt.contains("TileDimension") ? nbt.getInt("TileDimension") : -1);
	}

	@Override
	public void write(NbtCompound nbt, DimensionPalette dimensions) {
		super.write(nbt, dimensions);
		nbt.put("TilePos", NbtHelper.fromBlockPos(blockEntityPos));
		nbt.putInt("TileDimension", dimensions.encode(blockEntityDimension));
	}

}

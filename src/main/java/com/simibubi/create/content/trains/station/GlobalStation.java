package com.simibubi.create.content.trains.station;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SingleBlockEntityEdgePoint;

public class GlobalStation extends SingleBlockEntityEdgePoint {

	public String name;
	public WeakReference<Train> nearestTrain;
	public boolean assembling;

	public GlobalStation() {
		name = "Track Station";
		nearestTrain = new WeakReference<Train>(null);
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		super.blockEntityAdded(blockEntity, front);
		BlockState state = blockEntity.getCachedState();
		assembling =
			state != null && state.contains(StationBlock.ASSEMBLING) && state.get(StationBlock.ASSEMBLING);
	}

	@Override
	public void read(NbtCompound nbt, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, migration, dimensions);
		name = nbt.getString("Name");
		assembling = nbt.getBoolean("Assembling");
		nearestTrain = new WeakReference<Train>(null);
	}

	@Override
	public void read(PacketByteBuf buffer, DimensionPalette dimensions) {
		super.read(buffer, dimensions);
		name = buffer.readString();
		assembling = buffer.readBoolean();
		if (buffer.readBoolean())
			blockEntityPos = buffer.readBlockPos();
	}

	@Override
	public void write(NbtCompound nbt, DimensionPalette dimensions) {
		super.write(nbt, dimensions);
		nbt.putString("Name", name);
		nbt.putBoolean("Assembling", assembling);
	}

	@Override
	public void write(PacketByteBuf buffer, DimensionPalette dimensions) {
		super.write(buffer, dimensions);
		buffer.writeString(name);
		buffer.writeBoolean(assembling);
		buffer.writeBoolean(blockEntityPos != null);
		if (blockEntityPos != null)
			buffer.writeBlockPos(blockEntityPos);
	}

	public boolean canApproachFrom(TrackNode side) {
		return isPrimary(side) && !assembling;
	}

	@Override
	public boolean canNavigateVia(TrackNode side) {
		return super.canNavigateVia(side) && !assembling;
	}

	public void reserveFor(Train train) {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null
			|| nearestTrain.navigation.distanceToDestination > train.navigation.distanceToDestination)
			this.nearestTrain = new WeakReference<>(train);
	}

	public void cancelReservation(Train train) {
		if (nearestTrain.get() == train)
			nearestTrain = new WeakReference<>(null);
	}

	public void trainDeparted(Train train) {
		cancelReservation(train);
	}

	@Nullable
	public Train getPresentTrain() {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null || nearestTrain.getCurrentStation() != this)
			return null;
		return nearestTrain;
	}

	@Nullable
	public Train getImminentTrain() {
		Train nearestTrain = getNearestTrain();
		if (nearestTrain == null)
			return nearestTrain;
		if (nearestTrain.getCurrentStation() == this)
			return nearestTrain;
		if (!nearestTrain.navigation.isActive())
			return null;
		if (nearestTrain.navigation.distanceToDestination > 30)
			return null;
		return nearestTrain;
	}

	@Nullable
	public Train getNearestTrain() {
		return this.nearestTrain.get();
	}

}

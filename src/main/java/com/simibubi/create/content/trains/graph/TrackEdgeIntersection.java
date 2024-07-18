package com.simibubi.create.content.trains.graph;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import com.simibubi.create.foundation.utility.Couple;

public class TrackEdgeIntersection {

	public double location;
	public Couple<TrackNodeLocation> target;
	public double targetLocation;
	public UUID groupId;
	public UUID id;

	public TrackEdgeIntersection() {
		id = UUID.randomUUID();
	}

	public boolean isNear(double location) {
		return Math.abs(location - this.location) < 1 / 32f;
	}

	public boolean targets(TrackNodeLocation target1, TrackNodeLocation target2) {
		return target1.equals(target.getFirst()) && target2.equals(target.getSecond())
			|| target1.equals(target.getSecond()) && target2.equals(target.getFirst());
	}

	public NbtCompound write(DimensionPalette dimensions) {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("Id", id);
		if (groupId != null)
			nbt.putUuid("GroupId", groupId);
		nbt.putDouble("Location", location);
		nbt.putDouble("TargetLocation", targetLocation);
		nbt.put("TargetEdge", target.serializeEach(loc -> loc.write(dimensions)));
		return nbt;
	}

	public static TrackEdgeIntersection read(NbtCompound nbt, DimensionPalette dimensions) {
		TrackEdgeIntersection intersection = new TrackEdgeIntersection();
		intersection.id = nbt.getUuid("Id");
		if (nbt.contains("GroupId"))
			intersection.groupId = nbt.getUuid("GroupId");
		intersection.location = nbt.getDouble("Location");
		intersection.targetLocation = nbt.getDouble("TargetLocation");
		intersection.target = Couple.deserializeEach(nbt.getList("TargetEdge", NbtElement.COMPOUND_TYPE),
			tag -> TrackNodeLocation.read(tag, dimensions));
		return intersection;
	}

}

package com.simibubi.create.content.trains.entity;

import java.util.Map.Entry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.VecHelper;

public class TrainMigration {

	Couple<TrackNodeLocation> locations;
	double positionOnOldEdge;
	boolean curve;
	Vec3d fallback;

	public TrainMigration() {}

	public TrainMigration(TravellingPoint point) {
		double t = point.position / point.edge.getLength();
		fallback = point.edge.getPosition(null, t);
		curve = point.edge.isTurn();
		positionOnOldEdge = point.position;
		locations = Couple.create(point.node1.getLocation(), point.node2.getLocation());
	}

	public TrackGraphLocation tryMigratingTo(TrackGraph graph) {
		TrackNode node1 = graph.locateNode(locations.getFirst());
		TrackNode node2 = graph.locateNode(locations.getSecond());
		if (node1 != null && node2 != null) {
			TrackEdge edge = graph.getConnectionsFrom(node1)
				.get(node2);
			if (edge != null) {
				TrackGraphLocation graphLocation = new TrackGraphLocation();
				graphLocation.graph = graph;
				graphLocation.edge = locations;
				graphLocation.position = positionOnOldEdge;
				return graphLocation;
			}
		}

		if (curve)
			return null;

		Vec3d prevDirection = locations.getSecond()
			.getLocation()
			.subtract(locations.getFirst()
				.getLocation())
			.normalize();

		for (TrackNodeLocation loc : graph.getNodes()) {
			Vec3d nodeVec = loc.getLocation();
			if (nodeVec.squaredDistanceTo(fallback) > 32 * 32)
				continue;

			TrackNode newNode1 = graph.locateNode(loc);
			for (Entry<TrackNode, TrackEdge> entry : graph.getConnectionsFrom(newNode1)
				.entrySet()) {
				TrackEdge edge = entry.getValue();
				if (edge.isTurn())
					continue;
				TrackNode newNode2 = entry.getKey();
				float radius = 1 / 64f;
				Vec3d direction = edge.getDirection(true);
				if (!MathHelper.approximatelyEquals(direction.dotProduct(prevDirection), 1))
					continue;
				Vec3d intersectSphere = VecHelper.intersectSphere(nodeVec, direction, fallback, radius);
				if (intersectSphere == null)
					continue;
				if (!MathHelper.approximatelyEquals(direction.dotProduct(intersectSphere.subtract(nodeVec)
					.normalize()), 1))
					continue;
				double edgeLength = edge.getLength();
				double position = intersectSphere.distanceTo(nodeVec) - radius;
				if (Double.isNaN(position))
					continue;
				if (position < 0)
					continue;
				if (position > edgeLength)
					continue;

				TrackGraphLocation graphLocation = new TrackGraphLocation();
				graphLocation.graph = graph;
				graphLocation.edge = Couple.create(loc, newNode2.getLocation());
				graphLocation.position = position;
				return graphLocation;
			}
		}

		return null;
	}

	public NbtCompound write(DimensionPalette dimensions) {
		NbtCompound tag = new NbtCompound();
		tag.putBoolean("Curve", curve);
		tag.put("Fallback", VecHelper.writeNBT(fallback));
		tag.putDouble("Position", positionOnOldEdge);
		tag.put("Nodes", locations.serializeEach(l -> l.write(dimensions)));
		return tag;
	}

	public static TrainMigration read(NbtCompound tag, DimensionPalette dimensions) {
		TrainMigration trainMigration = new TrainMigration();
		trainMigration.curve = tag.getBoolean("Curve");
		trainMigration.fallback = VecHelper.readNBT(tag.getList("Fallback", NbtElement.DOUBLE_TYPE));
		trainMigration.positionOnOldEdge = tag.getDouble("Position");
		trainMigration.locations =
			Couple.deserializeEach(tag.getList("Nodes", NbtElement.COMPOUND_TYPE), c -> TrackNodeLocation.read(c, dimensions));
		return trainMigration;
	}

}
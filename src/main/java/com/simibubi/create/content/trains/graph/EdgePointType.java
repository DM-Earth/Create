package com.simibubi.create.content.trains.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.observer.TrackObserver;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.station.GlobalStation;

public class EdgePointType<T extends TrackEdgePoint> {

	public static final Map<Identifier, EdgePointType<?>> TYPES = new HashMap<>();
	private Identifier id;
	private Supplier<T> factory;

	public static final EdgePointType<SignalBoundary> SIGNAL =
		register(Create.asResource("signal"), SignalBoundary::new);
	public static final EdgePointType<GlobalStation> STATION =
		register(Create.asResource("station"), GlobalStation::new);
	public static final EdgePointType<TrackObserver> OBSERVER =
		register(Create.asResource("observer"), TrackObserver::new);

	public static <T extends TrackEdgePoint> EdgePointType<T> register(Identifier id, Supplier<T> factory) {
		EdgePointType<T> type = new EdgePointType<>(id, factory);
		TYPES.put(id, type);
		return type;
	}

	public EdgePointType(Identifier id, Supplier<T> factory) {
		this.id = id;
		this.factory = factory;
	}

	public T create() {
		T t = factory.get();
		t.setType(this);
		return t;
	}

	public Identifier getId() {
		return id;
	}
	
	public static TrackEdgePoint read(PacketByteBuf buffer, DimensionPalette dimensions) {
		Identifier type = buffer.readIdentifier();
		EdgePointType<?> edgePointType = TYPES.get(type);
		TrackEdgePoint point = edgePointType.create();
		point.read(buffer, dimensions);
		return point;
	}

}

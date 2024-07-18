package com.simibubi.create.content.trains.station;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

public interface StationMapData {

	boolean toggleStation(WorldAccess level, BlockPos pos, StationBlockEntity stationBlockEntity);

	void addStationMarker(StationMarker marker);

}

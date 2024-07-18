package com.simibubi.create.foundation.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Maps;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.station.StationMapData;
import com.simibubi.create.content.trains.station.StationMarker;

@Mixin(MapState.class)
public class MapItemSavedDataMixin implements StationMapData {
	@Unique
	private static final String STATION_MARKERS_KEY = "create:stations";

	@Shadow
	@Final
	public int centerX;

	@Shadow
	@Final
	public int centerZ;

	@Shadow
	@Final
	public byte scale;

	@Shadow
	@Final
	Map<String, MapIcon> icons;

	@Shadow
	private int iconCount;

	@Unique
	private final Map<String, StationMarker> create$stationMarkers = Maps.newHashMap();

	@Inject(
			method = "fromNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/item/map/MapState;",
			at = @At("RETURN")
	)
	private static void create$onLoad(NbtCompound compound, CallbackInfoReturnable<MapState> cir) {
		MapState mapData = cir.getReturnValue();
		StationMapData stationMapData = (StationMapData) mapData;

		NbtList listTag = compound.getList(STATION_MARKERS_KEY, NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < listTag.size(); ++i) {
			StationMarker stationMarker = StationMarker.load(listTag.getCompound(i));
			stationMapData.addStationMarker(stationMarker);
		}
	}

	@Inject(
			method = "writeNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/nbt/NbtCompound;",
			at = @At("RETURN")
	)
	public void create$onSave(NbtCompound compound, CallbackInfoReturnable<NbtCompound> cir) {
		NbtList listTag = new NbtList();
		for (StationMarker stationMarker : create$stationMarkers.values()) {
			listTag.add(stationMarker.save());
		}
		compound.put(STATION_MARKERS_KEY, listTag);
	}

	@Override
	public void addStationMarker(StationMarker marker) {
		create$stationMarkers.put(marker.getId(), marker);

		int scaleMultiplier = 1 << scale;
		float localX = (marker.getTarget().getX() - centerX) / (float) scaleMultiplier;
		float localZ = (marker.getTarget().getZ() - centerZ) / (float) scaleMultiplier;

		if (localX < -63.0F || localX > 63.0F || localZ < -63.0F || localZ > 63.0F) {
			removeIcon(marker.getId());
			return;
		}

		byte localXByte = (byte) (int) (localX * 2.0F + 0.5F);
		byte localZByte = (byte) (int) (localZ * 2.0F + 0.5F);

		MapIcon decoration = new StationMarker.Decoration(localXByte, localZByte, marker.getName());
		MapIcon oldDecoration = icons.put(marker.getId(), decoration);
		if (!decoration.equals(oldDecoration)) {
			if (oldDecoration != null && oldDecoration.getType().shouldUseIconCountLimit()) {
				--iconCount;
			}

			if (decoration.getType().shouldUseIconCountLimit()) {
				++iconCount;
			}

			markIconsDirty();
		}
	}

	@Shadow
	private void removeIcon(String identifier) {
		throw new AssertionError();
	}

	@Shadow
	private void markIconsDirty() {
		throw new AssertionError();
	}

	@Shadow
	public boolean iconCountNotLessThan(int trackedCount) {
		throw new AssertionError();
	}

	@Override
	public boolean toggleStation(WorldAccess level, BlockPos pos, StationBlockEntity stationBlockEntity) {
		double xCenter = pos.getX() + 0.5D;
		double zCenter = pos.getZ() + 0.5D;
		int scaleMultiplier = 1 << scale;

		double localX = (xCenter - (double) centerX) / (double) scaleMultiplier;
		double localZ = (zCenter - (double) centerZ) / (double) scaleMultiplier;

		if (localX < -63.0D || localX > 63.0D || localZ < -63.0D || localZ > 63.0D)
			return false;

		StationMarker marker = StationMarker.fromWorld(level, pos);
		if (marker == null)
			return false;

		if (create$stationMarkers.remove(marker.getId(), marker)) {
			removeIcon(marker.getId());
			return true;
		}

		if (!iconCountNotLessThan(256)) {
			addStationMarker(marker);
			return true;
		}

		return false;
	}

	@Inject(
			method = "removeBanner(Lnet/minecraft/world/BlockView;II)V",
			at = @At("RETURN")
	)
	public void create$onCheckBanners(BlockView blockGetter, int x, int z, CallbackInfo ci) {
		create$checkStations(blockGetter, x, z);
	}

	@Unique
	private void create$checkStations(BlockView blockGetter, int x, int z) {
		Iterator<StationMarker> iterator = create$stationMarkers.values().iterator();
		List<StationMarker> newMarkers = new ArrayList<>();

		while (iterator.hasNext()) {
			StationMarker marker = iterator.next();
			if (marker.getTarget().getX() == x && marker.getTarget().getZ() == z) {
				StationMarker other = StationMarker.fromWorld(blockGetter, marker.getSource());
				if (!marker.equals(other)) {
					iterator.remove();
					removeIcon(marker.getId());

					if (other != null && marker.getTarget().equals(other.getTarget())) {
						newMarkers.add(other);
					}
				}
			}
		}

		for (StationMarker marker : newMarkers) {
			addStationMarker(marker);
		}
	}
}

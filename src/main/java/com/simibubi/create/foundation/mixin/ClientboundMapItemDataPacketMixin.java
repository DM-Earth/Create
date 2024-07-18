package com.simibubi.create.foundation.mixin;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.station.StationMarker;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

// random priority to prevent networking conflicts
@Mixin(value = MapUpdateS2CPacket.class, priority = 426)
public class ClientboundMapItemDataPacketMixin {
	@Shadow
	@Final
	private List<MapIcon> icons;

	@Unique
	private int[] create$stationIndices;

	@Inject(method = "<init>(IBZLjava/util/Collection;Lnet/minecraft/item/map/MapState$UpdateData;)V", at = @At("RETURN"))
	private void create$onInit(int mapId, byte scale, boolean locked, @Nullable Collection<MapIcon> decorations, @Nullable MapState.UpdateData colorPatch, CallbackInfo ci) {
		create$stationIndices = create$getStationIndices(this.icons);
	}

	@Unique
	private static int[] create$getStationIndices(List<MapIcon> decorations) {
		if (decorations == null) {
			return new int[0];
		}

		IntList indices = new IntArrayList();
		for (int i = 0; i < decorations.size(); i++) {
			MapIcon decoration = decorations.get(i);
			if (decoration instanceof StationMarker.Decoration) {
				indices.add(i);
			}
		}
		return indices.toIntArray();
	}

	@Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("RETURN"))
	private void create$onInit(PacketByteBuf buf, CallbackInfo ci) {
		create$stationIndices = buf.readIntArray();

		if (icons != null) {
			for (int i : create$stationIndices) {
				if (i >= 0 && i < icons.size()) {
					MapIcon decoration = icons.get(i);
					icons.set(i, StationMarker.Decoration.from(decoration));
				}
			}
		}
	}

	@Inject(method = "write(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("RETURN"))
	private void create$onWrite(PacketByteBuf buf, CallbackInfo ci) {
		buf.writeIntArray(create$stationIndices);
	}
}

package com.simibubi.create.content.trains.graph;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.simibubi.create.foundation.utility.NBTHelper;

public class DimensionPalette {

	List<RegistryKey<World>> gatheredDims;

	public DimensionPalette() {
		gatheredDims = new ArrayList<>();
	}

	public int encode(RegistryKey<World> dimension) {
		int indexOf = gatheredDims.indexOf(dimension);
		if (indexOf == -1) {
			indexOf = gatheredDims.size();
			gatheredDims.add(dimension);
		}
		return indexOf;
	}

	public RegistryKey<World> decode(int index) {
		if (gatheredDims.size() <= index || index < 0)
			return World.OVERWORLD;
		return gatheredDims.get(index);
	}

	public void send(PacketByteBuf buffer) {
		buffer.writeInt(gatheredDims.size());
		gatheredDims.forEach(rk -> buffer.writeIdentifier(rk.getValue()));
	}

	public static DimensionPalette receive(PacketByteBuf buffer) {
		DimensionPalette palette = new DimensionPalette();
		int length = buffer.readInt();
		for (int i = 0; i < length; i++)
			palette.gatheredDims.add(RegistryKey.of(RegistryKeys.WORLD, buffer.readIdentifier()));
		return palette;
	}

	public void write(NbtCompound tag) {
		tag.put("DimensionPalette", NBTHelper.writeCompoundList(gatheredDims, rk -> {
			NbtCompound c = new NbtCompound();
			c.putString("Id", rk.getValue()
				.toString());
			return c;
		}));
	}

	public static DimensionPalette read(NbtCompound tag) {
		DimensionPalette palette = new DimensionPalette();
		NBTHelper.iterateCompoundList(tag.getList("DimensionPalette", NbtElement.COMPOUND_TYPE), c -> palette.gatheredDims
			.add(RegistryKey.of(RegistryKeys.WORLD, new Identifier(c.getString("Id")))));
		return palette;
	}

}

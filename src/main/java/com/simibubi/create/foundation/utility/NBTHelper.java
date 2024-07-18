package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;

public class NBTHelper {

	public static void putMarker(NbtCompound nbt, String marker) {
		nbt.putBoolean(marker, true);
	}

	public static <T extends Enum<?>> T readEnum(NbtCompound nbt, String key, Class<T> enumClass) {
		T[] enumConstants = enumClass.getEnumConstants();
		if (enumConstants == null)
			throw new IllegalArgumentException("Non-Enum class passed to readEnum: " + enumClass.getName());
		if (nbt.contains(key, NbtElement.STRING_TYPE)) {
			String name = nbt.getString(key);
			for (T t : enumConstants) {
				if (t.name()
					.equals(name))
					return t;
			}
		}
		return enumConstants[0];
	}

	public static <T extends Enum<?>> void writeEnum(NbtCompound nbt, String key, T enumConstant) {
		nbt.putString(key, enumConstant.name());
	}

	public static <T> NbtList writeCompoundList(Iterable<T> list, Function<T, NbtCompound> serializer) {
		NbtList listNBT = new NbtList();
		list.forEach(t -> {
			NbtCompound apply = serializer.apply(t);
			if (apply == null)
				return;
			listNBT.add(apply);
		});
		return listNBT;
	}

	public static <T> List<T> readCompoundList(NbtList listNBT, Function<NbtCompound, T> deserializer) {
		List<T> list = new ArrayList<>(listNBT.size());
		listNBT.forEach(inbt -> list.add(deserializer.apply((NbtCompound) inbt)));
		return list;
	}

	public static <T> void iterateCompoundList(NbtList listNBT, Consumer<NbtCompound> consumer) {
		listNBT.forEach(inbt -> consumer.accept((NbtCompound) inbt));
	}

	public static NbtList writeItemList(Iterable<ItemStack> stacks) {
		return writeCompoundList(stacks, NBTSerializer::serializeNBTCompound);
	}

	public static List<ItemStack> readItemList(NbtList stacks) {
		return readCompoundList(stacks, ItemStack::fromNbt);
	}

	public static NbtList writeAABB(Box bb) {
		NbtList bbtag = new NbtList();
		bbtag.add(NbtFloat.of((float) bb.minX));
		bbtag.add(NbtFloat.of((float) bb.minY));
		bbtag.add(NbtFloat.of((float) bb.minZ));
		bbtag.add(NbtFloat.of((float) bb.maxX));
		bbtag.add(NbtFloat.of((float) bb.maxY));
		bbtag.add(NbtFloat.of((float) bb.maxZ));
		return bbtag;
	}

	public static Box readAABB(NbtList bbtag) {
		if (bbtag == null || bbtag.isEmpty())
			return null;
		return new Box(bbtag.getFloat(0), bbtag.getFloat(1), bbtag.getFloat(2), bbtag.getFloat(3),
			bbtag.getFloat(4), bbtag.getFloat(5));
	}

	public static NbtList writeVec3i(Vec3i vec) {
		NbtList tag = new NbtList();
		tag.add(NbtInt.of(vec.getX()));
		tag.add(NbtInt.of(vec.getY()));
		tag.add(NbtInt.of(vec.getZ()));
		return tag;
	}

	public static Vec3i readVec3i(NbtList tag) {
		return new Vec3i(tag.getInt(0), tag.getInt(1), tag.getInt(2));
	}

	@Nonnull
	public static NbtElement getINBT(NbtCompound nbt, String id) {
		NbtElement inbt = nbt.get(id);
		if (inbt != null)
			return inbt;
		return new NbtCompound();
	}
	
	public static NbtCompound intToCompound(int i) {
		NbtCompound compoundTag = new NbtCompound();
		compoundTag.putInt("V", i);
		return compoundTag;
	}
	
	public static int intFromCompound(NbtCompound compoundTag) {
		return compoundTag.getInt("V");
	}

	public static void writeResourceLocation(NbtCompound nbt, String key, Identifier location) {
		nbt.putString(key, location.toString());
	}

	public static Identifier readResourceLocation(NbtCompound nbt, String key) {
		return new Identifier(nbt.getString(key));
	}

}

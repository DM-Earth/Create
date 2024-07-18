package com.simibubi.create.content.kinetics.fan.processing;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

public class FanProcessingTypeRegistry {
	private static final Map<Identifier, FanProcessingType> TYPES = new Object2ReferenceOpenHashMap<>();
	private static final Map<FanProcessingType, Identifier> IDS = new Reference2ObjectOpenHashMap<>();
	private static final List<FanProcessingType> SORTED_TYPES = new ReferenceArrayList<>();
	private static final List<FanProcessingType> SORTED_TYPES_VIEW = Collections.unmodifiableList(SORTED_TYPES);

	public static void register(Identifier id, FanProcessingType type) {
		if (TYPES.put(id, type) != null) {
			throw new IllegalArgumentException("Tried to override FanProcessingType registration for id '" + id + "'. This is not supported!");
		}
		Identifier prevId = IDS.put(type, id);
		if (prevId != null) {
			throw new IllegalArgumentException("Tried to register same FanProcessingType instance for multiple ids '" + prevId + "' and '" + id + "'. This is not supported!");
		}
		insertSortedType(type, id);
	}

	private static void insertSortedType(FanProcessingType type, Identifier id) {
		int index = Collections.binarySearch(SORTED_TYPES, type, (type1, type2) -> type2.getPriority() - type1.getPriority());
		if (index >= 0) {
			throw new IllegalStateException();
		}
		SORTED_TYPES.add(-index - 1, type);
	}

	@Nullable
	public static FanProcessingType getType(Identifier id) {
		return TYPES.get(id);
	}

	public static FanProcessingType getTypeOrThrow(Identifier id) {
		FanProcessingType type = getType(id);
		if (type == null) {
			throw new IllegalArgumentException("Could not get FanProcessingType for id '" + id + "'!");
		}
		return type;
	}

	@Nullable
	public static Identifier getId(FanProcessingType type) {
		return IDS.get(type);
	}

	public static Identifier getIdOrThrow(FanProcessingType type) {
		Identifier id = getId(type);
		if (id == null) {
			throw new IllegalArgumentException("Could not get id for FanProcessingType " + type + "!");
		}
		return id;
	}

	public static List<FanProcessingType> getSortedTypesView() {
		return SORTED_TYPES_VIEW;
	}
}

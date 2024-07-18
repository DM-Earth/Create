package com.simibubi.create.content.kinetics.mechanicalArm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class ArmInteractionPointType {

	private static final Map<Identifier, ArmInteractionPointType> TYPES = new HashMap<>();
	private static final List<ArmInteractionPointType> SORTED_TYPES = new ArrayList<>();

	protected final Identifier id;

	public ArmInteractionPointType(Identifier id) {
		this.id = id;
	}

	public static void register(ArmInteractionPointType type) {
		Identifier id = type.getId();
		if (TYPES.containsKey(id))
			throw new IllegalArgumentException("Tried to override ArmInteractionPointType registration for id '" + id + "'. This is not supported!");
		TYPES.put(id, type);
		SORTED_TYPES.add(type);
		SORTED_TYPES.sort((t1, t2) -> t2.getPriority() - t1.getPriority());
	}

	@Nullable
	public static ArmInteractionPointType get(Identifier id) {
		return TYPES.get(id);
	}

	public static void forEach(Consumer<ArmInteractionPointType> action) {
		SORTED_TYPES.forEach(action);
	}

	@Nullable
	public static ArmInteractionPointType getPrimaryType(World level, BlockPos pos, BlockState state) {
		for (ArmInteractionPointType type : SORTED_TYPES)
			if (type.canCreatePoint(level, pos, state))
				return type;
		return null;
	}

	public final Identifier getId() {
		return id;
	}

	public abstract boolean canCreatePoint(World level, BlockPos pos, BlockState state);

	@Nullable
	public abstract ArmInteractionPoint createPoint(World level, BlockPos pos, BlockState state);

	public int getPriority() {
		return 0;
	}

}

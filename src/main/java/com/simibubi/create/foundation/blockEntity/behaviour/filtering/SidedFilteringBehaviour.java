package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;

public class SidedFilteringBehaviour extends FilteringBehaviour {

	Map<Direction, FilteringBehaviour> sidedFilters;
	private BiFunction<Direction, FilteringBehaviour, FilteringBehaviour> filterFactory;
	private Predicate<Direction> validDirections;

	public SidedFilteringBehaviour(SmartBlockEntity be, ValueBoxTransform.Sided sidedSlot,
		BiFunction<Direction, FilteringBehaviour, FilteringBehaviour> filterFactory,
		Predicate<Direction> validDirections) {
		super(be, sidedSlot);
		this.filterFactory = filterFactory;
		this.validDirections = validDirections;
		sidedFilters = new IdentityHashMap<>();
		updateFilterPresence();
	}

	@Override
	public void initialize() {
		super.initialize();
	}

	public FilteringBehaviour get(Direction side) {
		return sidedFilters.get(side);
	}

	public void updateFilterPresence() {
		Set<Direction> valid = new HashSet<>();
		for (Direction d : Iterate.directions)
			if (validDirections.test(d))
				valid.add(d);
		for (Direction d : Iterate.directions)
			if (valid.contains(d)) {
				if (!sidedFilters.containsKey(d))
					sidedFilters.put(d, filterFactory.apply(d, new FilteringBehaviour(blockEntity, slotPositioning)));
			} else if (sidedFilters.containsKey(d))
				removeFilter(d);
	}

	@Override
	public void write(NbtCompound nbt, boolean clientPacket) {
		nbt.put("Filters", NBTHelper.writeCompoundList(sidedFilters.entrySet(), entry -> {
			NbtCompound compound = new NbtCompound();
			compound.putInt("Side", entry.getKey()
				.getId());
			entry.getValue()
				.write(compound, clientPacket);
			return compound;
		}));
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(NbtCompound nbt, boolean clientPacket) {
		NBTHelper.iterateCompoundList(nbt.getList("Filters", NbtElement.COMPOUND_TYPE), compound -> {
			Direction face = Direction.byId(compound.getInt("Side"));
			if (sidedFilters.containsKey(face))
				sidedFilters.get(face)
					.read(compound, clientPacket);
		});
		super.read(nbt, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();
		sidedFilters.values()
			.forEach(FilteringBehaviour::tick);
	}

	@Override
	public boolean setFilter(Direction side, ItemStack stack) {
		if (!sidedFilters.containsKey(side))
			return true;
		sidedFilters.get(side)
			.setFilter(stack);
		return true;
	}

	@Override
	public ItemStack getFilter(Direction side) {
		if (!sidedFilters.containsKey(side))
			return ItemStack.EMPTY;
		return sidedFilters.get(side)
			.getFilter();
	}

	public boolean test(Direction side, ItemStack stack) {
		if (!sidedFilters.containsKey(side))
			return true;
		return sidedFilters.get(side)
				.test(stack);
	}

	@Override
	public void destroy() {
		sidedFilters.values()
				.forEach(FilteringBehaviour::destroy);
		super.destroy();
	}

	@Override
	public ItemRequirement getRequiredItems() {
		return sidedFilters.values().stream().reduce(
				ItemRequirement.NONE,
				(a, b) -> a.union(b.getRequiredItems()),
				(a, b) -> a.union(b)
		);
	}

	public void removeFilter(Direction side) {
		if (!sidedFilters.containsKey(side))
			return;
		sidedFilters.remove(side)
				.destroy();
	}

	public boolean testHit(Direction direction, Vec3d hit) {
		ValueBoxTransform.Sided sidedPositioning = (Sided) slotPositioning;
		BlockState state = blockEntity.getCachedState();
		Vec3d localHit = hit.subtract(Vec3d.of(blockEntity.getPos()));
		return sidedPositioning.fromSide(direction)
			.testHit(state, localHit);
	}

}

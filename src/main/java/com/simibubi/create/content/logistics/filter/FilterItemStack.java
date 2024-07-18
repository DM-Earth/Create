package com.simibubi.create.content.logistics.filter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;

public class FilterItemStack {

	private ItemStack filterItemStack;
	private boolean fluidExtracted;
	private FluidStack filterFluidStack;

	public static FilterItemStack of(ItemStack filter) {
		if (filter.hasNbt()) {
			if (AllItems.FILTER.isIn(filter))
				return new ListFilterItemStack(filter);
			if (AllItems.ATTRIBUTE_FILTER.isIn(filter))
				return new AttributeFilterItemStack(filter);
		}

		return new FilterItemStack(filter);
	}

	public static FilterItemStack of(NbtCompound tag) {
		return of(ItemStack.fromNbt(tag));
	}

	public static FilterItemStack empty() {
		return of(ItemStack.EMPTY);
	}

	public boolean isEmpty() {
		return filterItemStack.isEmpty();
	}

	public NbtCompound serializeNBT() {
		NbtCompound ret = new NbtCompound();
		filterItemStack.writeNbt(ret);
		return ret;
	}

	public ItemStack item() {
		return filterItemStack;
	}

	public FluidStack fluid(World level) {
		resolveFluid(level);
		return filterFluidStack;
	}

	public boolean isFilterItem() {
		return filterItemStack.getItem() instanceof FilterItem;
	}

	//

	public boolean test(World world, ItemStack stack) {
		return test(world, stack, false);
	}

	public boolean test(World world, FluidStack stack) {
		return test(world, stack, true);
	}

	public boolean test(World world, ItemStack stack, boolean matchNBT) {
		if (isEmpty())
			return true;
		return FilterItem.testDirect(filterItemStack, stack, matchNBT);
	}

	public boolean test(World world, FluidStack stack, boolean matchNBT) {
		if (isEmpty())
			return true;
		if (stack.isEmpty())
			return false;

		resolveFluid(world);

		if (filterFluidStack.isEmpty())
			return false;
		if (!matchNBT)
			return filterFluidStack.getFluid()
				.matchesType(stack.getFluid());
		return filterFluidStack.isFluidEqual(stack);
	}

	//

	private void resolveFluid(World world) {
		if (!fluidExtracted) {
			fluidExtracted = true;
			if (GenericItemEmptying.canItemBeEmptied(world, filterItemStack))
				filterFluidStack = GenericItemEmptying.emptyItem(world, filterItemStack, true)
				.getFirst();
		}
	}

	protected FilterItemStack(ItemStack filter) {
		filterItemStack = filter;
		filterFluidStack = FluidStack.EMPTY;
		fluidExtracted = false;
	}

	public static class ListFilterItemStack extends FilterItemStack {

		public List<FilterItemStack> containedItems;
		public boolean shouldRespectNBT;
		public boolean isBlacklist;

		protected ListFilterItemStack(ItemStack filter) {
			super(filter);
			boolean defaults = !filter.hasNbt();

			containedItems = new ArrayList<>();
			ItemStackHandler items = FilterItem.getFilterItems(filter);
			for (int i = 0; i < items.getSlots().size(); i++) {
				ItemStack stackInSlot = items.getStackInSlot(i);
				if (!stackInSlot.isEmpty())
					containedItems.add(FilterItemStack.of(stackInSlot));
			}

			shouldRespectNBT = !defaults ? false
				: filter.getNbt()
					.getBoolean("RespectNBT");
			isBlacklist = defaults ? false
				: filter.getNbt()
					.getBoolean("Blacklist");
		}

		@Override
		public boolean test(World world, ItemStack stack, boolean matchNBT) {
			if (containedItems.isEmpty())
				return super.test(world, stack, matchNBT);
			for (FilterItemStack filterItemStack : containedItems)
				if (filterItemStack.test(world, stack, shouldRespectNBT))
					return !isBlacklist;
			return isBlacklist;
		}

		@Override
		public boolean test(World world, FluidStack stack, boolean matchNBT) {
			for (FilterItemStack filterItemStack : containedItems)
				if (filterItemStack.test(world, stack, shouldRespectNBT))
					return !isBlacklist;
			return isBlacklist;
		}

	}

	public static class AttributeFilterItemStack extends FilterItemStack {

		public enum WhitelistMode {
			WHITELIST_DISJ, WHITELIST_CONJ, BLACKLIST;
		}

		public WhitelistMode whitelistMode;
		public List<Pair<ItemAttribute, Boolean>> attributeTests;

		protected AttributeFilterItemStack(ItemStack filter) {
			super(filter);
			boolean defaults = !filter.hasNbt();

			attributeTests = new ArrayList<>();
			whitelistMode = WhitelistMode.values()[defaults ? 0
				: filter.getNbt()
					.getInt("WhitelistMode")];

			NbtList attributes = defaults ? new NbtList()
				: filter.getNbt()
					.getList("MatchedAttributes", NbtElement.COMPOUND_TYPE);
			for (NbtElement inbt : attributes) {
				NbtCompound compound = (NbtCompound) inbt;
				ItemAttribute attribute = ItemAttribute.fromNBT(compound);
				if (attribute != null)
					attributeTests.add(Pair.of(attribute, compound.getBoolean("Inverted")));
			}
		}

		@Override
		public boolean test(World world, FluidStack stack, boolean matchNBT) {
			return false;
		}

		@Override
		public boolean test(World world, ItemStack stack, boolean matchNBT) {
			if (attributeTests.isEmpty())
				return super.test(world, stack, matchNBT);
			for (Pair<ItemAttribute, Boolean> test : attributeTests) {
				ItemAttribute attribute = test.getFirst();
				boolean inverted = test.getSecond();
				boolean matches = attribute.appliesTo(stack, world) != inverted;

				if (matches) {
					switch (whitelistMode) {
					case BLACKLIST:
						return false;
					case WHITELIST_CONJ:
						continue;
					case WHITELIST_DISJ:
						return true;
					}
				} else {
					switch (whitelistMode) {
					case BLACKLIST:
						continue;
					case WHITELIST_CONJ:
						return false;
					case WHITELIST_DISJ:
						continue;
					}
				}
			}

			switch (whitelistMode) {
			case BLACKLIST:
				return true;
			case WHITELIST_CONJ:
				return true;
			case WHITELIST_DISJ:
				return false;
			}

			return false;
		}

	}

}

package com.simibubi.create.content.schematics.requirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.simibubi.create.foundation.utility.NBTProcessors;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DirtPathBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.TurtleEggBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;

public class ItemRequirement {
	public static final ItemRequirement NONE = new ItemRequirement(Collections.emptyList());
	public static final ItemRequirement INVALID = new ItemRequirement(Collections.emptyList());

	protected List<StackRequirement> requiredItems;

	public ItemRequirement(List<StackRequirement> requiredItems) {
		this.requiredItems = requiredItems;
	}

	public ItemRequirement(StackRequirement stackRequirement) {
		this(List.of(stackRequirement));
	}

	public ItemRequirement(ItemUseType usage, ItemStack stack) {
		this(new StackRequirement(stack, usage));
	}

	public ItemRequirement(ItemUseType usage, Item item) {
		this(usage, new ItemStack(item));
	}

	public ItemRequirement(ItemUseType usage, List<ItemStack> requiredItems) {
		this(requiredItems.stream()
			.map(req -> new StackRequirement(req, usage))
			.collect(Collectors.toList()));
	}

	public static ItemRequirement of(BlockState state, BlockEntity be) {
		Block block = state.getBlock();

		ItemRequirement requirement;
		if (block instanceof ISpecialBlockItemRequirement specialBlock) {
			requirement = specialBlock.getRequiredItems(state, be);
		} else {
			requirement = defaultOf(state, be);
		}

		if (be instanceof ISpecialBlockEntityItemRequirement specialBE)
			requirement = requirement.union(specialBE.getRequiredItems(state));

		return requirement;
	}

	private static ItemRequirement defaultOf(BlockState state, BlockEntity be) {
		Block block = state.getBlock();
		if (block == Blocks.AIR)
			return NONE;

		Item item = block.asItem();
		if (item == Items.AIR)
			return INVALID;

		// double slab needs two items
		if (state.contains(Properties.SLAB_TYPE)
			&& state.get(Properties.SLAB_TYPE) == SlabType.DOUBLE)
			return new ItemRequirement(ItemUseType.CONSUME, new ItemStack(item, 2));
		if (block instanceof TurtleEggBlock)
			return new ItemRequirement(ItemUseType.CONSUME, new ItemStack(item, state.get(TurtleEggBlock.EGGS)
				.intValue()));
		if (block instanceof SeaPickleBlock)
			return new ItemRequirement(ItemUseType.CONSUME, new ItemStack(item, state.get(SeaPickleBlock.PICKLES)
				.intValue()));
		if (block instanceof SnowBlock)
			return new ItemRequirement(ItemUseType.CONSUME, new ItemStack(item, state.get(SnowBlock.LAYERS)
				.intValue()));
		if (block instanceof FarmlandBlock || block instanceof DirtPathBlock)
			return new ItemRequirement(ItemUseType.CONSUME, Items.DIRT);
		if (block instanceof AbstractBannerBlock && be instanceof BannerBlockEntity bannerBE)
			return new ItemRequirement(new StrictNbtStackRequirement(bannerBE.getPickStack(), ItemUseType.CONSUME));

		return new ItemRequirement(ItemUseType.CONSUME, item);
	}

	public static ItemRequirement of(Entity entity) {
		if (entity instanceof ISpecialEntityItemRequirement specialEntity)
			return specialEntity.getRequiredItems();

		if (entity instanceof ItemFrameEntity itemFrame) {
			ItemStack frame = new ItemStack(Items.ITEM_FRAME);
			ItemStack displayedItem = NBTProcessors.withUnsafeNBTDiscarded(itemFrame.getHeldItemStack());
			if (displayedItem.isEmpty())
				return new ItemRequirement(ItemUseType.CONSUME, Items.ITEM_FRAME);
			return new ItemRequirement(List.of(new ItemRequirement.StackRequirement(frame, ItemUseType.CONSUME),
				new ItemRequirement.StrictNbtStackRequirement(displayedItem, ItemUseType.CONSUME)));
		}

		if (entity instanceof ArmorStandEntity armorStand) {
			List<StackRequirement> requirements = new ArrayList<>();
			requirements.add(new StackRequirement(new ItemStack(Items.ARMOR_STAND), ItemUseType.CONSUME));
			armorStand.getItemsEquipped()
				.forEach(s -> requirements
					.add(new StrictNbtStackRequirement(NBTProcessors.withUnsafeNBTDiscarded(s), ItemUseType.CONSUME)));
			return new ItemRequirement(requirements);
		}

		return INVALID;
	}

	public boolean isEmpty() {
		return NONE == this;
	}

	public boolean isInvalid() {
		return INVALID == this;
	}

	public List<StackRequirement> getRequiredItems() {
		return requiredItems;
	}

	public ItemRequirement union(ItemRequirement other) {
		if (this.isInvalid() || other.isInvalid())
			return INVALID;
		if (this.isEmpty())
			return other;
		if (other.isEmpty())
			return this;

		return new ItemRequirement(Stream.concat(requiredItems.stream(), other.requiredItems.stream())
			.collect(Collectors.toList()));
	}

	public enum ItemUseType {
		CONSUME, DAMAGE
	}

	public static class StackRequirement {
		public final ItemStack stack;
		public final ItemUseType usage;

		public StackRequirement(ItemStack stack, ItemUseType usage) {
			this.stack = stack;
			this.usage = usage;
		}

		public boolean matches(ItemStack other) {
			return ItemStack.areItemsEqual(stack, other);
		}

		public boolean matches(ItemVariant variant) {
			return variant.getItem() == stack.getItem();
		}
	}

	public static class StrictNbtStackRequirement extends StackRequirement {
		public StrictNbtStackRequirement(ItemStack stack, ItemUseType usage) {
			super(stack, usage);
		}

		@Override
		public boolean matches(ItemStack other) {
			return ItemStack.canCombine(stack, other);
		}

		@Override
		public boolean matches(ItemVariant variant) {
			return variant.matches(stack);
		}
	}
}

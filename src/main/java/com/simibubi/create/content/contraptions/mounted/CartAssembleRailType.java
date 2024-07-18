package com.simibubi.create.content.contraptions.mounted;

import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.StringIdentifiable;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.entry.BlockEntry;

public enum CartAssembleRailType implements StringIdentifiable {
	
	REGULAR(Blocks.RAIL),
	POWERED_RAIL(Blocks.POWERED_RAIL),
	DETECTOR_RAIL(Blocks.DETECTOR_RAIL),
	ACTIVATOR_RAIL(Blocks.ACTIVATOR_RAIL),
	CONTROLLER_RAIL(AllBlocks.CONTROLLER_RAIL)
	
	;

	private final Supplier<Block> railBlockSupplier;
	private final Supplier<Item> railItemSupplier;

	CartAssembleRailType(Block block) {
		this.railBlockSupplier = () -> block;
		this.railItemSupplier = block::asItem;
	}

	CartAssembleRailType(BlockEntry<?> block) {
		this.railBlockSupplier = block::get;
		this.railItemSupplier = () -> block.get().asItem();
	}

	public Block getBlock() {
		return railBlockSupplier.get();
	}

	public Item getItem() {
		return railItemSupplier.get();
	}
	
	public boolean matches(BlockState rail) {
		return rail.getBlock() == railBlockSupplier.get();
	}
	
	@Override
	public String asString() {
		return Lang.asId(name());
	}

}

package com.simibubi.create.content.contraptions.bearing;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;

public class BlankSailBlockItem extends BlockItem {
	public BlankSailBlockItem(Block block, Settings properties) {
		super(block, properties);
	}

	@Override
	public void appendBlocks(Map<Block, Item> blockToItemMap, Item item) {
		super.appendBlocks(blockToItemMap, item);
		for (BlockEntry<SailBlock> entry : AllBlocks.DYED_SAILS) {
			blockToItemMap.put(entry.get(), item);
		}
	}
}

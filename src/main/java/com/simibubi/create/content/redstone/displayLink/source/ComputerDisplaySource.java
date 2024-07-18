package com.simibubi.create.content.redstone.displayLink.source;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.utility.Components;

public class ComputerDisplaySource extends DisplaySource {

	@Override
	public List<MutableText> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
		List<MutableText> components = new ArrayList<>();
		NbtList tag = context.sourceConfig().getList("ComputerSourceList", NbtElement.STRING_TYPE);

		for (int i = 0; i < tag.size(); i++) {
			components.add(Components.literal(tag.getString(i)));
		}

		return components;
	}

	@Override
	public boolean shouldPassiveReset() {
		return false;
	}

}

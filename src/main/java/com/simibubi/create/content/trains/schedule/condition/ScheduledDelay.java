package com.simibubi.create.content.trains.schedule.condition;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ScheduledDelay extends TimedWaitCondition {

	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(ItemStack.EMPTY, Lang.translateDirect("schedule.condition.delay_short", formatTime(true)));
	}

	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		int time = context.getInt("Time");
		if (time >= totalWaitTicks())
			return true;
		
		context.putInt("Time", time + 1);
		requestDisplayIfNecessary(context, time);
		return false;
	}

	@Override
	public Identifier getId() {
		return Create.asResource("delay");
	}

}
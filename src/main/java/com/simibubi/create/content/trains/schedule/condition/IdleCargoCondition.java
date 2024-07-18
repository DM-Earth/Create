package com.simibubi.create.content.trains.schedule.condition;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class IdleCargoCondition extends TimedWaitCondition {
	
	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(ItemStack.EMPTY, Lang.translateDirect("schedule.condition.idle_short", formatTime(true)));
	}

	@Override
	public Identifier getId() {
		return Create.asResource("idle");
	}
	
	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		int idleTime = Integer.MAX_VALUE;
		for (Carriage carriage : train.carriages) 
			idleTime = Math.min(idleTime, carriage.storage.getTicksSinceLastExchange());
		context.putInt("Time", idleTime);
		requestDisplayIfNecessary(context, idleTime);
		return idleTime > totalWaitTicks();
	}
	
}
package com.simibubi.create.content.trains.schedule.condition;

import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleDataEntry;
import com.simibubi.create.foundation.utility.Pair;

public abstract class ScheduleWaitCondition extends ScheduleDataEntry {

	public abstract boolean tickCompletion(World level, Train train, NbtCompound context);

	protected void requestStatusToUpdate(NbtCompound context) {
		context.putInt("StatusVersion", context.getInt("StatusVersion") + 1);
	}

	public final NbtCompound write() {
		NbtCompound tag = new NbtCompound();
		NbtCompound dataCopy = data.copy();
		writeAdditional(dataCopy);
		tag.putString("Id", getId().toString());
		tag.put("Data", dataCopy);
		return tag;
	}

	public static ScheduleWaitCondition fromTag(NbtCompound tag) {
		Identifier location = new Identifier(tag.getString("Id"));
		Supplier<? extends ScheduleWaitCondition> supplier = null;
		for (Pair<Identifier, Supplier<? extends ScheduleWaitCondition>> pair : Schedule.CONDITION_TYPES)
			if (pair.getFirst()
				.equals(location))
				supplier = pair.getSecond();

		if (supplier == null) {
			Create.LOGGER.warn("Could not parse waiting condition type: " + location);
			return null;
		}

		ScheduleWaitCondition condition = supplier.get();
		// Left around for migration purposes. Data added in writeAdditional has moved into the "Data" tag
		condition.readAdditional(tag);
		NbtCompound data = tag.getCompound("Data");
		condition.readAdditional(data);
		condition.data = data;
		return condition;
	}

	public abstract MutableText getWaitingStatus(World level, Train train, NbtCompound tag);

}

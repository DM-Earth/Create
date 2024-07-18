package com.simibubi.create.content.trains.schedule.destination;

import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleDataEntry;
import com.simibubi.create.foundation.utility.Pair;

public abstract class ScheduleInstruction extends ScheduleDataEntry {

	public abstract boolean supportsConditions();

	public final NbtCompound write() {
		NbtCompound tag = new NbtCompound();
		NbtCompound dataCopy =  data.copy();
		writeAdditional(dataCopy);
		tag.putString("Id", getId().toString());
		tag.put("Data", dataCopy);
		return tag;
	}

	public static ScheduleInstruction fromTag(NbtCompound tag) {
		Identifier location = new Identifier(tag.getString("Id"));
		Supplier<? extends ScheduleInstruction> supplier = null;
		for (Pair<Identifier, Supplier<? extends ScheduleInstruction>> pair : Schedule.INSTRUCTION_TYPES)
			if (pair.getFirst()
				.equals(location))
				supplier = pair.getSecond();

		if (supplier == null) {
			Create.LOGGER.warn("Could not parse schedule instruction type: " + location);
			return new DestinationInstruction();
		}

		ScheduleInstruction scheduleDestination = supplier.get();
		// Left around for migration purposes. Data added in writeAdditional has moved into the "Data" tag
		scheduleDestination.readAdditional(tag);
		NbtCompound data = tag.getCompound("Data");
		scheduleDestination.readAdditional(data);
		scheduleDestination.data = data;
		return scheduleDestination;
	}

}

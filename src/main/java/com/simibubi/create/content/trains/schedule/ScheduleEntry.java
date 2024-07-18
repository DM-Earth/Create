package com.simibubi.create.content.trains.schedule;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.utility.NBTHelper;

public class ScheduleEntry {
	public ScheduleInstruction instruction;
	public List<List<ScheduleWaitCondition>> conditions;

	public ScheduleEntry() {
		conditions = new ArrayList<>();
	}

	public ScheduleEntry clone() {
		return fromTag(write());
	}

	public NbtCompound write() {
		NbtCompound tag = new NbtCompound();
		NbtList outer = new NbtList();
		tag.put("Instruction", instruction.write());
		if (!instruction.supportsConditions())
			return tag;
		for (List<ScheduleWaitCondition> column : conditions)
			outer.add(NBTHelper.writeCompoundList(column, ScheduleWaitCondition::write));
		tag.put("Conditions", outer);
		return tag;
	}

	public static ScheduleEntry fromTag(NbtCompound tag) {
		ScheduleEntry entry = new ScheduleEntry();
		entry.instruction = ScheduleInstruction.fromTag(tag.getCompound("Instruction"));
		entry.conditions = new ArrayList<>();
		if (entry.instruction.supportsConditions())
			for (NbtElement t : tag.getList("Conditions", NbtElement.LIST_TYPE))
				if (t instanceof NbtList list)
					entry.conditions.add(NBTHelper.readCompoundList(list, ScheduleWaitCondition::fromTag));
		return entry;
	}

}
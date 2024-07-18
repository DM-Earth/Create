package com.simibubi.create.content.trains.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.schedule.condition.FluidThresholdCondition;
import com.simibubi.create.content.trains.schedule.condition.IdleCargoCondition;
import com.simibubi.create.content.trains.schedule.condition.ItemThresholdCondition;
import com.simibubi.create.content.trains.schedule.condition.PlayerPassengerCondition;
import com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.condition.StationPoweredCondition;
import com.simibubi.create.content.trains.schedule.condition.StationUnloadedCondition;
import com.simibubi.create.content.trains.schedule.condition.TimeOfDayCondition;
import com.simibubi.create.content.trains.schedule.destination.ChangeThrottleInstruction;
import com.simibubi.create.content.trains.schedule.destination.ChangeTitleInstruction;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.Pair;

public class Schedule {

	public static List<Pair<Identifier, Supplier<? extends ScheduleInstruction>>> INSTRUCTION_TYPES =
		new ArrayList<>();
	public static List<Pair<Identifier, Supplier<? extends ScheduleWaitCondition>>> CONDITION_TYPES =
		new ArrayList<>();

	static {
		registerInstruction("destination", DestinationInstruction::new);
		registerInstruction("rename", ChangeTitleInstruction::new);
		registerInstruction("throttle", ChangeThrottleInstruction::new);
		registerCondition("delay", ScheduledDelay::new);
		registerCondition("time_of_day", TimeOfDayCondition::new);
		registerCondition("fluid_threshold", FluidThresholdCondition::new);
		registerCondition("item_threshold", ItemThresholdCondition::new);
		registerCondition("redstone_link", RedstoneLinkCondition::new);
		registerCondition("player_count", PlayerPassengerCondition::new);
		registerCondition("idle", IdleCargoCondition::new);
		registerCondition("unloaded", StationUnloadedCondition::new);
		registerCondition("powered", StationPoweredCondition::new);
	}

	private static void registerInstruction(String name, Supplier<? extends ScheduleInstruction> factory) {
		INSTRUCTION_TYPES.add(Pair.of(Create.asResource(name), factory));
	}

	private static void registerCondition(String name, Supplier<? extends ScheduleWaitCondition> factory) {
		CONDITION_TYPES.add(Pair.of(Create.asResource(name), factory));
	}

	public static <T> List<? extends Text> getTypeOptions(List<Pair<Identifier, T>> list) {
		String langSection = list.equals(INSTRUCTION_TYPES) ? "instruction." : "condition.";
		return list.stream()
			.map(Pair::getFirst)
			.map(rl -> rl.getNamespace() + ".schedule." + langSection + rl.getPath())
			.map(Components::translatable)
			.toList();
	}

	public List<ScheduleEntry> entries;
	public boolean cyclic;
	public int savedProgress;

	public Schedule() {
		entries = new ArrayList<>();
		cyclic = true;
		savedProgress = 0;
	}

	public NbtCompound write() {
		NbtCompound tag = new NbtCompound();
		NbtList list = NBTHelper.writeCompoundList(entries, ScheduleEntry::write);
		tag.put("Entries", list);
		tag.putBoolean("Cyclic", cyclic);
		if (savedProgress > 0)
			tag.putInt("Progress", savedProgress);
		return tag;
	}

	public static Schedule fromTag(NbtCompound tag) {
		Schedule schedule = new Schedule();
		schedule.entries = NBTHelper.readCompoundList(tag.getList("Entries", NbtElement.COMPOUND_TYPE), ScheduleEntry::fromTag);
		schedule.cyclic = tag.getBoolean("Cyclic");
		if (tag.contains("Progress"))
			schedule.savedProgress = tag.getInt("Progress");
		return schedule;
	}

}

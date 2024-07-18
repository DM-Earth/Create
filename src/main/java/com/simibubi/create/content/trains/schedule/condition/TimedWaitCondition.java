package com.simibubi.create.content.trains.schedule.condition;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public abstract class TimedWaitCondition extends ScheduleWaitCondition {

	public static enum TimeUnit {
		TICKS(1, "t", "generic.unit.ticks"),
		SECONDS(20, "s", "generic.unit.seconds"),
		MINUTES(20 * 60, "min", "generic.unit.minutes");

		public int ticksPer;
		public String suffix;
		public String key;

		private TimeUnit(int ticksPer, String suffix, String key) {
			this.ticksPer = ticksPer;
			this.suffix = suffix;
			this.key = key;
		}

		public static List<Text> translatedOptions() {
			return Lang.translatedOptions(null, TICKS.key, SECONDS.key, MINUTES.key);
		}
	}

	protected void requestDisplayIfNecessary(NbtCompound context, int time) {
		int ticksUntilDeparture = totalWaitTicks() - time;
		if (ticksUntilDeparture < 20 * 60 && ticksUntilDeparture % 100 == 0)
			requestStatusToUpdate(context);
		if (ticksUntilDeparture >= 20 * 60 && ticksUntilDeparture % (20 * 60) == 0)
			requestStatusToUpdate(context);
	}

	public int totalWaitTicks() {
		return getValue() * getUnit().ticksPer;
	}

	public TimedWaitCondition() {
		data.putInt("Value", 5);
		data.putInt("TimeUnit", TimeUnit.SECONDS.ordinal());
	}

	protected Text formatTime(boolean compact) {
		if (compact)
			return Components.literal(getValue() + getUnit().suffix);
		return Components.literal(getValue() + " ").append(Lang.translateDirect(getUnit().key));
	}

	@Override
	public List<Text> getTitleAs(String type) {
		return ImmutableList.of(
			Components.translatable(getId().getNamespace() + ".schedule." + type + "." + getId().getPath()),
			Lang.translateDirect("schedule.condition.for_x_time", formatTime(false))
				.formatted(Formatting.DARK_AQUA));
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return new ItemStack(Items.REPEATER);
	}

	@Override
	public List<Text> getSecondLineTooltip(int slot) {
		return ImmutableList.of(Lang.translateDirect("generic.duration"));
	}

	public int getValue() {
		return intData("Value");
	}

	public TimeUnit getUnit() {
		return enumData("TimeUnit", TimeUnit.class);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 31, (i, l) -> {
			i.titled(Lang.translateDirect("generic.duration"))
				.withShiftStep(15)
				.withRange(0, 121);
			i.lockedTooltipX = -15;
			i.lockedTooltipY = 35;
		}, "Value");

		builder.addSelectionScrollInput(36, 85, (i, l) -> {
			i.forOptions(TimeUnit.translatedOptions())
				.titled(Lang.translateDirect("generic.timeUnit"));
		}, "TimeUnit");
	}

	@Override
	public MutableText getWaitingStatus(World level, Train train, NbtCompound tag) {
		int time = tag.getInt("Time");
		int ticksUntilDeparture = totalWaitTicks() - time;
		boolean showInMinutes = ticksUntilDeparture >= 20 * 60;
		int num =
			(int) (showInMinutes ? Math.floor(ticksUntilDeparture / (20 * 60f)) : Math.ceil(ticksUntilDeparture / 100f) * 5);
		String key = "generic." + (showInMinutes ? num == 1 ? "daytime.minute" : "unit.minutes"
			: num == 1 ? "daytime.second" : "unit.seconds");
		return Lang.translateDirect("schedule.condition." + getId().getPath() + ".status",
			Components.literal(num + " ").append(Lang.translateDirect(key)));
	}

}

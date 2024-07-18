package com.simibubi.create.content.trains.schedule.condition;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PlayerPassengerCondition extends ScheduleWaitCondition {

	@Override
	public Pair<ItemStack, Text> getSummary() {
		int target = getTarget();
		return Pair.of(AllBlocks.SEATS.get(DyeColor.YELLOW)
			.asStack(),
			Lang.translateDirect("schedule.condition.player_count." + (target == 1 ? "summary" : "summary_plural"), target));
	}

	@Override
	public Identifier getId() {
		return Create.asResource("player_count");
	}

	public int getTarget() {
		return intData("Count");
	}

	public boolean canOvershoot() {
		return intData("Exact") != 0;
	}

	@Override
	public List<Text> getTitleAs(String type) {
		int target = getTarget();
		return ImmutableList.of(Lang.translateDirect("schedule.condition.player_count.seated",
			Lang.translateDirect("schedule.condition.player_count." + (target == 1 ? "summary" : "summary_plural"),
				Components.literal("" + target).formatted(Formatting.DARK_AQUA))));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 31, (i, l) -> {
			i.titled(Lang.translateDirect("schedule.condition.player_count.players"))
				.withShiftStep(5)
				.withRange(0, 21);
		}, "Count");

		builder.addSelectionScrollInput(36, 85, (i, l) -> {
			i.forOptions(Lang.translatedOptions("schedule.condition.player_count", "exactly", "or_above"))
				.titled(Lang.translateDirect("schedule.condition.player_count.condition"));
		}, "Exact");
	}

	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		int prev = context.getInt("PrevPlayerCount");
		int present = train.countPlayerPassengers();
		int target = getTarget();
		context.putInt("PrevPlayerCount", present);
		if (prev != present)
			requestStatusToUpdate(context);
		return canOvershoot() ? present >= target : present == target;
	}

	@Override
	public MutableText getWaitingStatus(World level, Train train, NbtCompound tag) {
		return Lang.translateDirect("schedule.condition.player_count.status", train.countPlayerPassengers(), getTarget());
	}

}

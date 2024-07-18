package com.simibubi.create.content.trains.schedule.condition;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import io.github.fabricators_of_create.porting_lib.util.NBTSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class RedstoneLinkCondition extends ScheduleWaitCondition {

	public Couple<Frequency> freq;

	public RedstoneLinkCondition() {
		freq = Couple.create(() -> Frequency.EMPTY);
	}

	@Override
	public int slotsTargeted() {
		return 2;
	}

	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(AllBlocks.REDSTONE_LINK.asStack(),
			lowActivation() ? Lang.translateDirect("schedule.condition.redstone_link_off")
				: Lang.translateDirect("schedule.condition.redstone_link_on"));
	}

	@Override
	public List<Text> getSecondLineTooltip(int slot) {
		return ImmutableList.of(Lang.translateDirect(slot == 0 ? "logistics.firstFrequency" : "logistics.secondFrequency")
			.formatted(Formatting.RED));
	}

	@Override
	public List<Text> getTitleAs(String type) {
		return ImmutableList.of(
			Lang.translateDirect("schedule.condition.redstone_link.frequency_" + (lowActivation() ? "unpowered" : "powered")),
			Components.literal(" #1 ").formatted(Formatting.GRAY)
				.append(freq.getFirst()
					.getStack()
					.getName()
					.copy()
					.formatted(Formatting.DARK_AQUA)),
			Components.literal(" #2 ").formatted(Formatting.GRAY)
				.append(freq.getSecond()
					.getStack()
					.getName()
					.copy()
					.formatted(Formatting.DARK_AQUA)));
	}

	@Override
	public boolean tickCompletion(World level, Train train, NbtCompound context) {
		int lastChecked = context.contains("LastChecked") ? context.getInt("LastChecked") : -1;
		int status = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
		if (status == lastChecked)
			return false;
		context.putInt("LastChecked", status);
		return Create.REDSTONE_LINK_NETWORK_HANDLER.hasAnyLoadedPower(freq) != lowActivation();
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		freq.set(slot == 0, Frequency.of(stack));
		super.setItem(slot, stack);
	}

	@Override
	public ItemStack getItem(int slot) {
		return freq.get(slot == 0)
			.getStack();
	}

	@Override
	public Identifier getId() {
		return Create.asResource("redstone_link");
	}

	@Override
	protected void writeAdditional(NbtCompound tag) {
		tag.put("Frequency", freq.serializeEach(f -> NBTSerializer.serializeNBTCompound(f.getStack())));
	}

	public boolean lowActivation() {
		return intData("Inverted") == 1;
	}

	@Override
	protected void readAdditional(NbtCompound tag) {
		if (tag.contains("Frequency"))
			freq = Couple.deserializeEach(tag.getList("Frequency", NbtElement.COMPOUND_TYPE), c -> Frequency.of(ItemStack.fromNbt(c)));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addSelectionScrollInput(20, 101,
			(i, l) -> i.forOptions(Lang.translatedOptions("schedule.condition.redstone_link", "powered", "unpowered"))
				.titled(Lang.translateDirect("schedule.condition.redstone_link.frequency_state")),
			"Inverted");
	}

	@Override
	public MutableText getWaitingStatus(World level, Train train, NbtCompound tag) {
		return Lang.translateDirect("schedule.condition.redstone_link.status");
	}

}

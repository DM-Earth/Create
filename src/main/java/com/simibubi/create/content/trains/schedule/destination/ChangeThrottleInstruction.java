package com.simibubi.create.content.trains.schedule.destination;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ChangeThrottleInstruction extends ScheduleInstruction {

	public ChangeThrottleInstruction() {
		super();
		data.putInt("Value", 100);
	}

	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(icon(), formatted());
	}

	private MutableText formatted() {
		return Components.literal(intData("Value") + "%");
	}

	@Override
	public Identifier getId() {
		return Create.asResource("throttle");
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return icon();
	}

	@Override
	public boolean supportsConditions() {
		return false;
	}

	@Override
	public List<Text> getTitleAs(String type) {
		return ImmutableList.of(Lang
			.translateDirect("schedule." + type + "." + getId().getPath() + ".summary",
				formatted().formatted(Formatting.WHITE))
			.formatted(Formatting.GOLD));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 50, (si, l) -> {
			si.withRange(5, 101)
				.withStepFunction(c -> c.shift ? 25 : 5)
				.titled(Lang.translateDirect("schedule.instruction.throttle_edit_box"));
			l.withSuffix("%");
		}, "Value");
	}

	public float getThrottle() {
		return intData("Value") / 100f;
	}

	private ItemStack icon() {
		return AllBlocks.TRAIN_CONTROLS.asStack();
	}

	@Override
	public List<Text> getSecondLineTooltip(int slot) {
		return ImmutableList.of(Lang.translateDirect("schedule.instruction.throttle_edit_box"),
			Lang.translateDirect("schedule.instruction.throttle_edit_box_1")
				.formatted(Formatting.GRAY));
	}

}

package com.simibubi.create.content.trains.schedule.destination;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class DestinationInstruction extends TextScheduleInstruction {

	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(AllBlocks.TRACK_STATION.asStack(), Components.literal(getLabelText()));
	}

	@Override
	public boolean supportsConditions() {
		return true;
	}

	@Override
	public Identifier getId() {
		return Create.asResource("destination");
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return AllBlocks.TRACK_STATION.asStack();
	}

	public String getFilter() {
		return getLabelText();
	}
	
	public String getFilterForRegex() {
		String filter = getFilter();
		if (filter.isBlank())
			return filter;
		return "\\Q" + filter.replace("*", "\\E.*\\Q") + "\\E";
	}

	@Override
	public List<Text> getSecondLineTooltip(int slot) {
		return ImmutableList.of(Lang.translateDirect("schedule.instruction.filter_edit_box"),
			Lang.translateDirect("schedule.instruction.filter_edit_box_1")
				.formatted(Formatting.GRAY),
			Lang.translateDirect("schedule.instruction.filter_edit_box_2")
				.formatted(Formatting.DARK_GRAY),
			Lang.translateDirect("schedule.instruction.filter_edit_box_3")
				.formatted(Formatting.DARK_GRAY));
	}

	@Override
	@Environment(EnvType.CLIENT)
	protected void modifyEditBox(TextFieldWidget box) {
		box.setTextPredicate(s -> StringUtils.countMatches(s, '*') <= 3);
	}

}
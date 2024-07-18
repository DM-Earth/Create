package com.simibubi.create.content.trains.schedule.destination;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

public class ChangeTitleInstruction extends TextScheduleInstruction {

	@Override
	public Pair<ItemStack, Text> getSummary() {
		return Pair.of(icon(), Components.literal(getLabelText()));
	}

	@Override
	public Identifier getId() {
		return Create.asResource("rename");
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return icon();
	}

	@Override
	public boolean supportsConditions() {
		return false;
	}
	
	public String getScheduleTitle() {
		return getLabelText();
	}

	private ItemStack icon() {
		return new ItemStack(Items.NAME_TAG);
	}

	@Override
	public List<Text> getSecondLineTooltip(int slot) {
		return ImmutableList.of(Lang.translateDirect("schedule.instruction.name_edit_box"),
			Lang.translateDirect("schedule.instruction.name_edit_box_1")
				.formatted(Formatting.GRAY),
			Lang.translateDirect("schedule.instruction.name_edit_box_2")
				.formatted(Formatting.DARK_GRAY));
	}

}

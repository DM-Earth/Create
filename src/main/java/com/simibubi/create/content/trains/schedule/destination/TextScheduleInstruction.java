package com.simibubi.create.content.trains.schedule.destination;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class TextScheduleInstruction extends ScheduleInstruction {

	protected String getLabelText() {
		return textData("Text");
	}

	@Override
	public List<Text> getTitleAs(String type) {
		return ImmutableList.of(Lang.translateDirect("schedule." + type + "." + getId().getPath() + ".summary")
			.formatted(Formatting.GOLD), Lang.translateDirect("generic.in_quotes", Components.literal(getLabelText())));
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addTextInput(0, 121, (e, t) -> modifyEditBox(e), "Text");
	}

	@Environment(EnvType.CLIENT)
	protected void modifyEditBox(TextFieldWidget box) {}

}

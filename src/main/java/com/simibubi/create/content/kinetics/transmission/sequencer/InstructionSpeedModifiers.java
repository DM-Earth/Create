package com.simibubi.create.content.kinetics.transmission.sequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.text.Text;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

public enum InstructionSpeedModifiers {

	FORWARD_FAST(2, ">>"), FORWARD(1, "->"), BACK(-1, "<-"), BACK_FAST(-2, "<<"),

	;

	String translationKey;
	int value;
	Text label;

	private InstructionSpeedModifiers(int modifier, Text label) {
		this.label = label;
		translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(name());
		value = modifier;
	}
	private InstructionSpeedModifiers(int modifier, String label) {
		this.label = Components.literal(label);
		translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(name());
		value = modifier;
	}

	static List<Text> getOptions() {
		List<Text> options = new ArrayList<>();
		for (InstructionSpeedModifiers entry : values())
			options.add(Lang.translateDirect(entry.translationKey));
		return options;
	}

	public static InstructionSpeedModifiers getByModifier(int modifier) {
		return Arrays.stream(InstructionSpeedModifiers.values())
				.filter(speedModifier -> speedModifier.value == modifier)
				.findAny()
				.orElse(InstructionSpeedModifiers.FORWARD);
	}

}

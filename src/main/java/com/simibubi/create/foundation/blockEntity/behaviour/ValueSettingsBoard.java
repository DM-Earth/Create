package com.simibubi.create.foundation.blockEntity.behaviour;

import java.util.List;
import net.minecraft.text.Text;

public record ValueSettingsBoard(Text title, int maxValue, int milestoneInterval, List<Text> rows,
	ValueSettingsFormatter formatter) {
}

package com.simibubi.create.content.equipment.goggles;

import java.util.List;
import net.minecraft.text.Text;

/*
* Implement this Interface in the BlockEntity class that wants to add info to the screen
* */
public interface IHaveHoveringInformation {

	default boolean addToTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
		return false;
	}

}

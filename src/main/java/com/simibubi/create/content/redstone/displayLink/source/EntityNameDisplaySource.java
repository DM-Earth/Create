package com.simibubi.create.content.redstone.displayLink.source;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Box;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

public class EntityNameDisplaySource extends SingleLineDisplaySource {
	@Override
	protected MutableText provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		List<SeatEntity> seats = context.level().getNonSpectatingEntities(SeatEntity.class, new Box(context.getSourcePos()));

		if (seats.isEmpty())
			return EMPTY_LINE;

		SeatEntity seatEntity = seats.get(0);
		List<Entity> passengers = seatEntity.getPassengerList();

		if (passengers.isEmpty())
			return EMPTY_LINE;

		return passengers.get(0).getDisplayName().copy();
	}

	@Override
	protected String getTranslationKey() {
		return "entity_name";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}
}

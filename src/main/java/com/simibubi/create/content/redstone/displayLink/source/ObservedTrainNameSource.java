package com.simibubi.create.content.redstone.displayLink.source;

import java.util.UUID;
import net.minecraft.text.MutableText;
import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.observer.TrackObserver;
import com.simibubi.create.content.trains.observer.TrackObserverBlockEntity;

public class ObservedTrainNameSource extends SingleLineDisplaySource {

	@Override
	protected MutableText provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		if (!(context.getSourceBlockEntity() instanceof TrackObserverBlockEntity observerBE))
			return EMPTY_LINE;
		TrackObserver observer = observerBE.getObserver();
		if (observer == null)
			return EMPTY_LINE;
		UUID currentTrain = observer.getCurrentTrain();
		if (currentTrain == null)
			return EMPTY_LINE;
		Train train = Create.RAILWAYS.trains.get(currentTrain);
		if (train == null)
			return EMPTY_LINE;
		return train.name.copy();
	}
	
	@Override
	public int getPassiveRefreshTicks() {
		return 400;
	}
	
	@Override
	protected String getTranslationKey() {
		return "observed_train_name";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

}

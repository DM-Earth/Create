package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import java.util.List;
import java.util.function.Function;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

public class BulkScrollValueBehaviour extends ScrollValueBehaviour {

	Function<SmartBlockEntity, List<? extends SmartBlockEntity>> groupGetter;

	public BulkScrollValueBehaviour(Text label, SmartBlockEntity be, ValueBoxTransform slot,
		Function<SmartBlockEntity, List<? extends SmartBlockEntity>> groupGetter) {
		super(label, be, slot);
		this.groupGetter = groupGetter;
	}

	@Override
	public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlDown) {
		if (!ctrlDown) {
			super.setValueSettings(player, valueSetting, ctrlDown);
			return;
		}
		if (!valueSetting.equals(getValueSettings()))
			playFeedbackSound(this);
		for (SmartBlockEntity be : getBulk()) {
			ScrollValueBehaviour other = be.getBehaviour(ScrollValueBehaviour.TYPE);
			if (other != null)
				other.setValue(valueSetting.value());
		}
	}

	public List<? extends SmartBlockEntity> getBulk() {
		return groupGetter.apply(blockEntity);
	}

}

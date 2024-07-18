package com.simibubi.create.content.kinetics.motor;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;

public class KineticScrollValueBehaviour extends ScrollValueBehaviour {

	public KineticScrollValueBehaviour(Text label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(label, be, slot);
		withFormatter(v -> String.valueOf(Math.abs(v)));
	}

	@Override
	public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
		ImmutableList<Text> rows = ImmutableList.of(Components.literal("\u27f3")
			.formatted(Formatting.BOLD),
			Components.literal("\u27f2")
				.formatted(Formatting.BOLD));
		ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
		return new ValueSettingsBoard(label, 256, 32, rows, formatter);
	}

	@Override
	public void setValueSettings(PlayerEntity player, ValueSettings valueSetting, boolean ctrlHeld) {
		int value = Math.max(1, valueSetting.value());
		if (!valueSetting.equals(getValueSettings()))
			playFeedbackSound(this);
		setValue(valueSetting.row() == 0 ? -value : value);
	}

	@Override
	public ValueSettings getValueSettings() {
		return new ValueSettings(value < 0 ? 0 : 1, Math.abs(value));
	}

	public MutableText formatSettings(ValueSettings settings) {
		return Lang.number(Math.max(1, Math.abs(settings.value())))
			.add(Lang.text(settings.row() == 0 ? "\u27f3" : "\u27f2")
				.style(Formatting.BOLD))
			.component();
	}
	
	@Override
	public String getClipboardKey() {
		return "Speed";
	}

}
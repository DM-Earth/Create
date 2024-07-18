package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter.ScrollOptionSettingsFormatter;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;

public class ScrollOptionBehaviour<E extends Enum<E> & INamedIconOptions> extends ScrollValueBehaviour {

	private E[] options;

	public ScrollOptionBehaviour(Class<E> enum_, Text label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(label, be, slot);
		options = enum_.getEnumConstants();
		between(0, options.length - 1);
	}

	INamedIconOptions getIconForSelected() {
		return get();
	}

	public E get() {
		return options[value];
	}

	@Override
	public ValueSettingsBoard createBoard(PlayerEntity player, BlockHitResult hitResult) {
		return new ValueSettingsBoard(label, max, 1, ImmutableList.of(Components.literal("Select")),
			new ScrollOptionSettingsFormatter(options));
	}
	
	@Override
	public String getClipboardKey() {
		return options[0].getClass().getSimpleName();
	}

}

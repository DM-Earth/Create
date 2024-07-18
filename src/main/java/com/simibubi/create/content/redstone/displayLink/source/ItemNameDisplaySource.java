package com.simibubi.create.content.redstone.displayLink.source;

import org.apache.commons.lang3.mutable.MutableObject;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;

public class ItemNameDisplaySource extends SingleLineDisplaySource {

	@Override
	protected MutableText provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
		DisplayLinkBlockEntity gatherer = context.blockEntity();
		Direction direction = gatherer.getDirection();
		Mutable pos = gatherer.getSourcePosition()
			.mutableCopy();

		MutableText combined = EMPTY_LINE.copy();

		for (int i = 0; i < 32; i++) {
			TransportedItemStackHandlerBehaviour behaviour =
				BlockEntityBehaviour.get(context.level(), pos, TransportedItemStackHandlerBehaviour.TYPE);
			pos.move(direction);

			if (behaviour == null)
				break;

			MutableObject<ItemStack> stackHolder = new MutableObject<>();
			behaviour.handleCenteredProcessingOnAllItems(.25f, tis -> {
				stackHolder.setValue(tis.stack);
				return TransportedResult.doNothing();
			});

			ItemStack stack = stackHolder.getValue();
			if (stack != null && !stack.isEmpty())
				combined = combined.append(stack.getName());
		}

		return combined;
	}

	@Override
	protected String getTranslationKey() {
		return "combine_item_names";
	}

	@Override
	protected boolean allowsLabeling(DisplayLinkContext context) {
		return true;
	}

	@Override
	protected String getFlapDisplayLayoutName(DisplayLinkContext context) {
		return "Number";
	}

}

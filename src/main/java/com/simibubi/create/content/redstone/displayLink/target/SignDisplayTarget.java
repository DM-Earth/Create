package com.simibubi.create.content.redstone.displayLink.target;

import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.MutableText;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.utility.Iterate;

public class SignDisplayTarget extends DisplayTarget {

	@Override
	public void acceptText(int line, List<MutableText> text, DisplayLinkContext context) {
		BlockEntity be = context.getTargetBlockEntity();
		if (!(be instanceof SignBlockEntity sign))
			return;

		boolean changed = false;
		SignText signText = new SignText();
		for (int i = 0; i < text.size() && i + line < 4; i++) {
			if (i == 0)
				reserve(i + line, sign, context);
			if (i > 0 && isReserved(i + line, sign, context))
				break;

			signText.withMessage(i + line, text.get(i));
			changed = true;
		}

		if (changed)
			for (boolean side : Iterate.trueAndFalse)
				sign.setText(signText, side);
		context.level()
			.updateListeners(context.getTargetPos(), sign.getCachedState(), sign.getCachedState(), 2);
	}

	@Override
	public DisplayTargetStats provideStats(DisplayLinkContext context) {
		return new DisplayTargetStats(4, 15, this);
	}

}

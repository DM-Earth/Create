package com.simibubi.create.content.redstone.displayLink.target;

import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.utility.Lang;

public abstract class SingleLineDisplayTarget extends DisplayTarget {

	@Override
	public final void acceptText(int line, List<MutableText> text, DisplayLinkContext context) {
		acceptLine(text.get(0), context);
	}
	
	protected abstract void acceptLine(MutableText text, DisplayLinkContext context);

	@Override
	public final DisplayTargetStats provideStats(DisplayLinkContext context) {
		return new DisplayTargetStats(1, getWidth(context), this);
	}
	
	@Override
	public Text getLineOptionText(int line) {
		return Lang.translateDirect("display_target.single_line");
	}
	
	protected abstract int getWidth(DisplayLinkContext context);

}
